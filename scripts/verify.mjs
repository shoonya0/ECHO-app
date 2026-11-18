#!/usr/bin/env node
// scripts/verify.mjs: the one verify entry point for the ECHO Android app.
// Runs the fast checks in order and stops at the first failure; `--full` adds the slow ones.
//
//   node scripts/verify.mjs          # fast (~2 min warm): what the Stop hook runs
//   node scripts/verify.mjs --full   # fast + lint, UI-test compile, debug build, device tests: what CI runs
//
// .claude/settings.json sets CLAUDE_VERIFY_CMD to "node scripts/verify.mjs".
// Commands are constant strings from this file, run through the platform shell so
// gradlew.bat resolves on Windows; never build them from untrusted input.
// Timings measured 2026-11-18 (Windows 11, warm Gradle daemon, one Kotlin file changed):
// unit tests ~94 s, lint ~44 s, spotlessCheck ~5 s.

import { spawnSync } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
// Absolute and quoted: Claude Code sets NoDefaultCurrentDirectoryInExePath, so cmd.exe
// won't find a bare `gradlew.bat` in the working directory.
const GRADLE = `"${path.join(ROOT, process.platform === 'win32' ? 'gradlew.bat' : 'gradlew')}"`;
const G = (tasks) => `${GRADLE} ${tasks} --console=plain`;

const FAST = [
  { name: 'docs-check', cmd: 'node scripts/docs-check.mjs' },
  { name: 'architecture', cmd: 'node scripts/check-architecture.mjs' },
  { name: 'format (ktlint, changed files)', cmd: G('spotlessCheck') }, // fix: gradlew spotlessApply
  { name: 'compile + unit tests', cmd: G(':app:testDebugUnitTest') },
];

const SLOW = [
  { name: 'android lint', cmd: G(':app:lintDebug') },
  { name: 'compile UI tests', cmd: G(':app:compileDebugAndroidTestKotlin') },
  { name: 'debug build', cmd: G(':app:assembleDebug') },
  { name: 'UI tests (device)', cmd: G(':app:connectedDebugAndroidTest'), needsDevice: true },
];

// adb from ANDROID_HOME / ANDROID_SDK_ROOT, or sdk.dir in local.properties, or PATH.
function adbPath() {
  const exe = process.platform === 'win32' ? 'adb.exe' : 'adb';
  const roots = [process.env.ANDROID_HOME, process.env.ANDROID_SDK_ROOT];
  const props = path.join(ROOT, 'local.properties');
  if (existsSync(props)) {
    const m = /^sdk\.dir=(.*)$/m.exec(readFileSync(props, 'utf8'));
    if (m) roots.push(m[1].trim().replace(/\\:/g, ':').replace(/\\\\/g, '\\'));
  }
  for (const r of roots.filter(Boolean)) {
    const p = path.join(r, 'platform-tools', exe);
    if (existsSync(p)) return p;
  }
  return exe;
}

function deviceAttached() {
  const r = spawnSync(adbPath(), ['devices'], { encoding: 'utf8', windowsHide: true, timeout: 20_000 });
  if (r.error || r.status !== 0) return false;
  return r.stdout.split(/\r?\n/).slice(1).some((l) => /\tdevice\s*$/.test(l));
}

const steps = process.argv.includes('--full') ? [...FAST, ...SLOW] : FAST;
const summary = [];

for (const step of steps) {
  if (step.needsDevice && !deviceAttached()) {
    summary.push(`- ${step.name}: SKIPPED (no emulator or device attached; NOT verified)`);
    continue;
  }
  const started = Date.now();
  process.stdout.write(`\n▶ ${step.name}: ${step.cmd}\n`);
  const r = spawnSync(step.cmd, { cwd: ROOT, shell: true, stdio: 'inherit', windowsHide: true });
  const seconds = ((Date.now() - started) / 1000).toFixed(1);
  const ok = r.status === 0 && !r.error;
  summary.push(`${ok ? '✔' : '✘'} ${step.name} (${seconds}s)${r.error ? ` — ${r.error.message}` : ''}`);
  if (!ok) {
    process.stdout.write(`\n${summary.join('\n')}\nverify FAILED at "${step.name}"\n`);
    process.exit(r.status || 1);
  }
}

process.stdout.write(`\n${summary.join('\n')}\nverify passed (${steps.length} steps)\n`);
