#!/usr/bin/env node
// scripts/check-architecture.mjs: import-boundary check for the ECHO Android app.
// Enforces the layers table in _docs/architecture-overview.md#layers-and-allowed-imports.
// Every message says how to fix the violation. Node built-ins only; same on Windows and macOS.
//
//   node scripts/check-architecture.mjs           # exit 1 on any violation
//   node scripts/check-architecture.mjs --json    # machine output
//
// It reads the `import com.shoonya.echo.…` and framework imports of every .kt file under
// app/src/main/java/com/shoonya/echo. Test sources are not checked.
// The baseline (2026-11-18, 201396e) had zero violations, so violations fail from day one.

import { readdirSync, readFileSync, statSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const SRC = path.join(ROOT, 'app', 'src', 'main', 'java', 'com', 'shoonya', 'echo');
const DOC = '_docs/architecture-overview.md#layers-and-allowed-imports';

// Allowed exceptions, each with a reason. Keep this list short; remove an entry when fixed.
const EXCEPTIONS = [
  {
    file: 'features/auth/data/repository/AuthRepositoryImpl.kt',
    imports: 'com.shoonya.echo.features.chat.data.local.ChatCache',
    why: 'logout clears the chat cache (existing at setup, 2026-11-18)',
  },
];

// Where a file sits: { area: 'feature'|'core'|'shell', feature, layer }
function locate(rel) {
  const parts = rel.split('/');
  if (parts[0] === 'features') return { area: 'feature', feature: parts[1], layer: parts[2] };
  if (parts[0] === 'core') return { area: 'core', feature: null, layer: parts[1] };
  return { area: 'shell', feature: null, layer: null }; // EchoApplication, MainActivity, MainNavigation
}

// What an import points at, for com.shoonya.echo.* imports.
function target(imp) {
  const m = /^com\.shoonya\.echo\.(features)\.([a-z0-9_]+)\.([a-z0-9_]+)/.exec(imp);
  if (m) return { area: 'feature', feature: m[2], layer: m[3] };
  const c = /^com\.shoonya\.echo\.core\.([a-z0-9_]+)/.exec(imp);
  if (c) return { area: 'core', feature: null, layer: c[1] };
  return null;
}

const FRAMEWORK = /^(android|androidx|retrofit2|okhttp3)\./;

function rules(from, imp) {
  const to = target(imp);
  // domain must stay plain Kotlin: no Android, Retrofit or OkHttp types.
  if (from.layer === 'domain' && FRAMEWORK.test(imp)) {
    return 'domain code must be plain Kotlin. Move the Android/Retrofit/OkHttp type behind an interface in domain/ and implement it in data/.';
  }
  if (!to) return null;
  if (from.area === 'core' && to.area === 'feature') {
    return 'core/ is feature-agnostic. Move the shared type into core/ (e.g. core/domain), or move this code into the feature.';
  }
  if (from.area !== 'feature') return null;
  if (from.layer === 'domain' && (to.layer === 'data' || to.layer === 'presentation' ||
      (to.area === 'core' && to.layer !== 'domain'))) {
    return 'domain may import only domain code and core/domain. Define an interface in domain/ and implement it in data/.';
  }
  if (from.layer === 'presentation' && (to.layer === 'data')) {
    return 'presentation must not import data/. Call a use case (domain/usecase) or a domain repository interface instead.';
  }
  if (from.layer === 'data' && to.layer === 'presentation') {
    return 'data must not import presentation/. Return domain models and let the ViewModel map them to UI state.';
  }
  if (to.area === 'feature' && to.feature !== from.feature) {
    if (to.layer === 'data') {
      return `feature "${from.feature}" must not import feature "${to.feature}"'s data/. Use its domain/ (repository interface or use case), or move the shared piece into core/.`;
    }
    if (to.layer === 'presentation' && from.feature !== 'home') {
      return `only features/home (the tab host) may import another feature's screens. Navigate through MainNavigation.kt routes, or move the shared composable into core/presentation.`;
    }
  }
  return null;
}

function walk(dir, out = []) {
  for (const name of readdirSync(dir)) {
    const full = path.join(dir, name);
    if (statSync(full).isDirectory()) walk(full, out);
    else if (name.endsWith('.kt')) out.push(full);
  }
  return out;
}

const findings = [];
for (const file of walk(SRC)) {
  const rel = path.relative(SRC, file).split(path.sep).join('/');
  const from = locate(rel);
  const lines = readFileSync(file, 'utf8').split(/\r?\n/);
  lines.forEach((text, i) => {
    const m = /^import\s+([\w.]+)/.exec(text);
    if (!m) return;
    const imp = m[1];
    const msg = rules(from, imp);
    if (!msg) return;
    if (EXCEPTIONS.some((e) => e.file === rel && imp.startsWith(e.imports))) return;
    findings.push({ file: `app/src/main/java/com/shoonya/echo/${rel}`, line: i + 1, import: imp, msg });
  });
}

if (process.argv.includes('--json')) {
  process.stdout.write(JSON.stringify({ findings }, null, 2) + '\n');
} else {
  for (const f of findings) process.stdout.write(`ERROR ${f.file}:${f.line}  import ${f.import}\n      ${f.msg} (see ${DOC})\n`);
  process.stdout.write(`check-architecture: ${findings.length} violation(s), ${EXCEPTIONS.length} listed exception(s)\n`);
}
process.exit(findings.length ? 1 : 0);
