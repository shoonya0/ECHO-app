#Requires -Version 5.1
<#
.SYNOPSIS
    Start N ECHO emulator instances (default 2) for two-user testing.

.DESCRIPTION
    Launches N Android emulators, each on its own console port, so each gets a
    stable adb serial: emulator-5554, emulator-5556, ... . ECHO's debug build
    calls http://10.0.2.2:8080 (the host's localhost), so start the Go backend
    first or login and chat will fail.

    Modes:
      Clone     (default) One AVD per user: <Template>-userA, <Template>-userB, ...
                Separate identities and quickboot snapshots, both windowed, each
                keeps its own logged-in account between runs.
      ReadOnly  All instances share the template AVD with -read-only (CI style).
                No extra disk, no snapshots, user data is discarded on shutdown.

    Ports are chosen dynamically: an emulator already running with the expected
    AVD name is reused instead of duplicated; otherwise the first free console
    port from 5554 upwards is used (5554, 5556, 5558, ...).

    Nothing here changes the app or its Gradle configuration; it only starts
    emulators and optionally installs the existing debug APK.

.PARAMETER Count
    Number of instances, 2..4.

.PARAMETER Mode
    Clone (default) or ReadOnly.

.PARAMETER TemplateAvd
    AVD used as the template; it must exist (Android Studio -> Device Manager).
    If it does not exist and the SDK has exactly one AVD, that one is used.

.PARAMETER Lite
    Clone mode only: writes 720x1560 @293dpi (same dp size as Pixel 5, 2.25x
    fewer pixels) and turns the cameras off. Ignored for AVDs that already exist.

.PARAMETER Headless
    Add -no-window to every instance. Pair with scrcpy to view one on demand.

.PARAMETER ColdBoot
    Clone mode only: -no-snapshot-load -no-snapshot-save (slower starts, no
    snapshot state).

.PARAMETER Install
    adb install -r -t the debug APK on every instance after boot.

.PARAMETER Launch
    Start com.Shoonya.Echo/com.shoonya.echo.MainActivity on every instance.

.PARAMETER Stop
    Kill every running emulator whose AVD is one of this script's AVD names.

.EXAMPLE
    .\local-testing\emulators.ps1 -Install -Launch
    Two windowed AVDs, app installed and started: log in as user A and user B.

.EXAMPLE
    .\local-testing\emulators.ps1 -Mode ReadOnly -Headless -Install
    One template AVD, two -read-only instances, no windows.

.EXAMPLE
    .\local-testing\emulators.ps1 -Stop

.NOTES
    Windows PowerShell 5.1 compatible. See ./README.md for the full guide, the
    lighter-image options and the two-device troubleshooting table.
#>
[CmdletBinding()]
param(
    [ValidateRange(2, 4)]
    [int]$Count = 2,

    [ValidateSet('Clone', 'ReadOnly')]
    [string]$Mode = 'Clone',

    [string]$TemplateAvd = 'Pixel_5',

    [switch]$Lite,

    [switch]$Headless,

    [switch]$ColdBoot,

    [switch]$Install,

    [switch]$Launch,

    [switch]$Stop,

    [ValidateSet('auto', 'host', 'angle_indirect', 'swiftshader_indirect', 'guest')]
    [string]$Gpu = 'angle_indirect',

    [int]$MemoryMb = 2048,

    [int]$Cores = 2,

    [int]$BootTimeoutSec = 300
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# ------------------------------------------------------------------ paths ---

$RepoRoot = Split-Path -Parent $PSScriptRoot
function Resolve-SdkRoot {
    param([Parameter(Mandatory)][string]$Root)

    $props = Join-Path $Root 'local.properties'
    if (Test-Path -LiteralPath $props) {
        $line = Select-String -LiteralPath $props -Pattern '^sdk\.dir=(.+)$' | Select-Object -First 1
        if ($line) {
            # local.properties escapes the drive colon and backslashes: D\:\\Applications\\AndroidSdk
            return $line.Matches[0].Groups[1].Value.Trim().Replace('\:', ':').Replace('\\', '\')
        }
    }
    foreach ($candidate in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT)) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) { return $candidate }
    }
    throw 'Android SDK not found: set sdk.dir in local.properties or ANDROID_HOME.'
}

$Sdk      = Resolve-SdkRoot -Root $RepoRoot
$Emulator = Join-Path $Sdk 'emulator\emulator.exe'
$Adb      = Join-Path $Sdk 'platform-tools\adb.exe'
$Apk      = Join-Path $RepoRoot 'app\build\outputs\apk\debug\app-debug.apk'
$Activity = 'com.Shoonya.Echo/com.shoonya.echo.MainActivity'

foreach ($exe in @($Emulator, $Adb)) {
    if (-not (Test-Path -LiteralPath $exe)) { throw "Not found: $exe" }
}

if ($env:ANDROID_AVD_HOME) {
    $AvdHome = $env:ANDROID_AVD_HOME
} else {
    $AvdHome = Join-Path $env:USERPROFILE '.android\avd'
}

$LiteWidth   = 720
$LiteHeight  = 1560
$LiteDensity = 293

# ---------------------------------------------------------------- helpers ---

function Test-PortOpen {
    param(
        [string]$TargetHost,
        [int]$Port,
        [int]$TimeoutMs = 400
    )
    $client = New-Object System.Net.Sockets.TcpClient
    try {
        return $client.ConnectAsync($TargetHost, $Port).Wait($TimeoutMs)
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

# Native calls with $ErrorActionPreference = 'Stop' terminate the script on ANY
# stderr output, and adb writes progress and "device not found" there. Every
# external command therefore goes through Invoke-NativeText, which diverts stderr
# instead of letting it throw.
function Invoke-NativeText {
    param(
        [Parameter(Mandatory)][string]$File,
        [Parameter(Mandatory)][string[]]$Arguments
    )
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $raw = & $File @Arguments 2>&1
    } finally {
        $ErrorActionPreference = $previous
    }
    # 2>&1 turns stderr into ErrorRecord objects; keep only real stdout text.
    return @($raw | Where-Object { $_ -is [string] })
}

function Invoke-Adb {
    param([Parameter(Mandatory)][string[]]$AdbArgs)
    return @(Invoke-NativeText -File $Adb -Arguments $AdbArgs)
}

function Get-AvdNames {
    $names = @()
    foreach ($line in @(Invoke-NativeText -File $Emulator -Arguments @('-list-avds'))) {
        $name = $line.Trim()
        if ($name -and -not $name.StartsWith('INFO', [StringComparison]::OrdinalIgnoreCase)) { $names += $name }
    }
    return $names
}

function Get-AdbDevices {
    $devices = @()
    foreach ($line in @(Invoke-Adb -AdbArgs @('devices'))) {
        if ($line -match '^(\S+)\s+(\w+)$') {
            $devices += [pscustomobject]@{ Serial = $Matches[1]; State = $Matches[2] }
        }
    }
    return $devices
}

function Get-OnlineSerials {
    return @(Get-AdbDevices | Where-Object { $_.State -eq 'device' } | ForEach-Object { $_.Serial })
}

function Invoke-AdbShell {
    param([string]$Serial, [string]$Command)
    return ((@(Invoke-Adb -AdbArgs @('-s', $Serial, 'shell', $Command)) -join "`n").Trim())
}

function Get-RunningEmulators {
    $result = @()
    foreach ($serial in (Get-OnlineSerials)) {
        $name = Invoke-AdbShell -Serial $serial -Command 'getprop ro.boot.qemu.avd_name'
        $result += [pscustomobject]@{ Serial = $serial; AvdName = $name }
    }
    return $result
}

function Wait-InstanceBoot {
    param(
        [Parameter(Mandatory)]$Instance,
        [int]$TimeoutSec = 300
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    $lastReport = Get-Date
    while ((Get-Date) -lt $deadline) {
        if ((Invoke-AdbShell -Serial $Instance.Serial -Command 'getprop sys.boot_completed') -eq '1') {
            return $true
        }
        if ($Instance.Process) {
            $Instance.Process.Refresh()
            if ($Instance.Process.HasExited) {
                throw ("[{0}] emulator exited with code {1}. Log: {2}" -f `
                    $Instance.Role, $Instance.Process.ExitCode, $Instance.Log)
            }
        }
        if (((Get-Date) - $lastReport).TotalSeconds -ge 30) {
            Write-Host ("[{0}] still booting {1} ..." -f $Instance.Role, $Instance.Serial)
            $lastReport = Get-Date
        }
        Start-Sleep -Seconds 3
    }
    return $false
}
function New-AvdClone {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][string]$Template,
        [Parameter(Mandatory)][string]$AvdRoot,
        [int]$RamMb = 2048,
        [int]$CpuCores = 2,
        [switch]$LiteProfile
    )

    $srcDir = Join-Path $AvdRoot "$Template.avd"
    $srcIni = Join-Path $AvdRoot "$Template.ini"
    if (-not (Test-Path -LiteralPath $srcDir)) { throw "Template AVD not found: $srcDir" }

    $dstDir = Join-Path $AvdRoot "$Name.avd"
    $dstIni = Join-Path $AvdRoot "$Name.ini"
    if (Test-Path -LiteralPath $dstDir) { return $false }

    # Minimal clone: only config.ini. The emulator regenerates userdata, cache,
    # hardware-qemu.ini and the quickboot snapshot on first start, so the copy
    # costs a couple of KB instead of gigabytes.
    New-Item -ItemType Directory -Path $dstDir -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $srcDir 'config.ini') -Destination (Join-Path $dstDir 'config.ini')

    $cfgPath = Join-Path $dstDir 'config.ini'
    $cfg = Get-Content -LiteralPath $cfgPath
    $cfg = $cfg -replace '^AvdId=.*$', "AvdId=$Name"
    $cfg = $cfg -replace '^hw\.cpu\.ncore=.*$', "hw.cpu.ncore=$CpuCores"
    $cfg = $cfg -replace '^hw\.ramSize=.*$', "hw.ramSize=$RamMb"
    if ($LiteProfile) {
        $cfg = $cfg -replace '^hw\.lcd\.width=.*$', "hw.lcd.width=$LiteWidth"
        $cfg = $cfg -replace '^hw\.lcd\.height=.*$', "hw.lcd.height=$LiteHeight"
        $cfg = $cfg -replace '^hw\.lcd\.density=.*$', "hw.lcd.density=$LiteDensity"
        $cfg = $cfg -replace '^showDeviceFrame=.*$', 'showDeviceFrame=no'
        $cfg = $cfg -replace '^hw\.camera\.back=.*$', 'hw.camera.back=none'
        $cfg = $cfg -replace '^hw\.camera\.front=.*$', 'hw.camera.front=none'
    }
    Set-Content -LiteralPath $cfgPath -Value $cfg -Encoding ASCII

    $target = 'android-34'
    if (Test-Path -LiteralPath $srcIni) {
        $t = Select-String -LiteralPath $srcIni -Pattern '^target=(.+)$' | Select-Object -First 1
        if ($t) { $target = $t.Matches[0].Groups[1].Value.Trim() }
    }
    Set-Content -LiteralPath $dstIni -Encoding ASCII -Value @(
        'avd.ini.encoding=UTF-8'
        "path=$dstDir"
        "path.rel=avd\$Name.avd"
        "target=$target"
    )
    return $true
}

# --------------------------------------------------------------- template ---

if (-not (Test-Path -LiteralPath (Join-Path $AvdHome "$TemplateAvd.avd"))) {
    $available = @(Get-AvdNames)
    if ($available.Count -eq 1) {
        Write-Host ("template AVD '{0}' not found - using the only AVD in the SDK: '{1}'" -f $TemplateAvd, $available[0])
        $TemplateAvd = $available[0]
    } elseif ($available.Count -eq 0) {
        throw "No AVDs found in $AvdHome. Create one in Android Studio's Device Manager first."
    } else {
        throw ("AVD '{0}' not found. Available: {1}. Pick one with -TemplateAvd <name>." -f `
            $TemplateAvd, ($available -join ', '))
    }
}

# -------------------------------------------------------------- instances ---

$instances = @()
for ($i = 0; $i -lt $Count; $i++) {
    $letter = [char](65 + $i)                        # A, B, C, D
    if ($Mode -eq 'ReadOnly') {
        $avdName = $TemplateAvd
    } else {
        $avdName = '{0}-user{1}' -f $TemplateAvd, $letter
    }
    $instances += [pscustomobject]@{
        Role        = "user$letter"
        Index       = $i
        AvdName     = $avdName
        ConsolePort = 0
        AdbPort     = 0
        Serial      = ''
        Reused      = $false
        BootChecked = $false
        Log         = ''
        Process     = $null
    }
}

$knownAvdNames = @($instances | ForEach-Object { $_.AvdName } | Select-Object -Unique)

Invoke-Adb -AdbArgs @('start-server') | Out-Null

# ------------------------------------------------------------------- stop ---

if ($Stop) {
    $killed = 0
    $handledPorts = @()
    foreach ($emu in (Get-RunningEmulators)) {
        $port = $emu.Serial -replace '^emulator-', ''
        $handledPorts += $port
        if ($knownAvdNames -contains $emu.AvdName) {
            Write-Host ("stopping {0} ({1})" -f $emu.Serial, $emu.AvdName)
            Invoke-Adb -AdbArgs @('-s', $emu.Serial, 'emu', 'kill') | Out-Null
            $killed++
        }
    }

    # A hung instance never reaches adb, so it is invisible above. Fall back to the
    # process list, but only for ports we did not already ask to shut down (that
    # would kill a graceful exit that is still writing its quickboot snapshot).
    Start-Sleep -Seconds 2
    $forced = 0
    $procs = @(Get-CimInstance Win32_Process -Filter "Name='qemu-system-x86_64.exe' OR Name='qemu-system-x86_64-headless.exe'" -ErrorAction SilentlyContinue)
    foreach ($proc in $procs) {
        if (-not $proc.CommandLine) { continue }
        $portMatch = [regex]::Match($proc.CommandLine, '-port\s+(\d+)')
        if ($portMatch.Success -and $handledPorts -contains $portMatch.Groups[1].Value) { continue }
        foreach ($name in $knownAvdNames) {
            $pattern = '-avd\s+"?' + [regex]::Escape($name) + '"?(\s|$)'
            if ($proc.CommandLine -match $pattern) {
                Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
                Write-Host ("killed unresponsive emulator process {0} ({1})" -f $proc.ProcessId, $name)
                $forced++
                break
            }
        }
    }
    Write-Host "stopped $killed instance(s) gracefully, force-killed $forced process(es)"
    return
}

# ------------------------------------------------------------------- plan ---

foreach ($inst in $instances) {
    if ($Mode -eq 'Clone') {
        $created = New-AvdClone -Name $inst.AvdName -Template $TemplateAvd -AvdRoot $AvdHome `
            -RamMb $MemoryMb -CpuCores $Cores -LiteProfile:$Lite
        if ($created) {
            $note = ''
            if ($Lite) { $note = ', 720x1560' }
            Write-Host ("[{0}] created AVD {1} (ram={2}MB, cores={3}{4})" -f `
                $inst.Role, $inst.AvdName, $MemoryMb, $Cores, $note)
        }
    }
}

$running = @(Get-RunningEmulators)
$occupiedSerials = @(Get-AdbDevices | ForEach-Object { $_.Serial })
$claimed = @()

$booting = @(Get-AdbDevices | Where-Object { $_.State -ne 'device' -and $_.Serial -like 'emulator-*' })
if ($booting.Count -gt 0) {
    Write-Warning ("{0} emulator(s) are not online yet ({1}). Their AVD name is unknown until adb reports 'device', so this run starts new instances on other ports instead of reusing them. Re-run once they are booted to reuse them." -f `
        $booting.Count, (($booting | ForEach-Object { "{0}={1}" -f $_.Serial, $_.State }) -join ', '))
}

foreach ($inst in $instances) {
    $match = $running | Where-Object { $_.AvdName -eq $inst.AvdName -and $claimed -notcontains $_.Serial } |
        Select-Object -First 1

    if ($match) {
        $inst.Serial      = $match.Serial
        $inst.ConsolePort = [int]($match.Serial -replace '^emulator-', '')
        $inst.AdbPort     = $inst.ConsolePort + 1
        $inst.Reused      = $true
        $claimed         += $match.Serial
    } else {
        $takenSerials = @($occupiedSerials) + $claimed
        $port = 5554
        while ($true) {
            $serial = "emulator-$port"
            $busy = $false
            if ($takenSerials -contains $serial) { $busy = $true }
            if (Test-PortOpen -TargetHost '127.0.0.1' -Port $port) { $busy = $true }
            if (Test-PortOpen -TargetHost '127.0.0.1' -Port ($port + 1)) { $busy = $true }
            if (-not $busy) { break }
            $port += 2
        }
        $inst.Serial      = $serial
        $inst.ConsolePort = $port
        $inst.AdbPort     = $port + 1
        $claimed         += $serial
    }
}

$extra = @($running | Where-Object { $claimed -notcontains $_.Serial })
if ($extra.Count -gt 0) {
    $list = ($extra | ForEach-Object { "{0}={1}" -f $_.Serial, $_.AvdName }) -join ', '
    Write-Warning ("{0} other emulator(s) already running ({1}). Each costs ~2 GB of RAM; stop them if the host gets short on memory." -f $extra.Count, $list)
}

if (-not (Test-PortOpen -TargetHost '127.0.0.1' -Port 8080)) {
    Write-Warning 'Nothing listens on host port 8080. The debug app calls http://10.0.2.2:8080, so start the ECHO backend before logging in.'
}

# ------------------------------------------------------------------ start ---

$logDir = Join-Path $env:TEMP 'echo-emulators'
New-Item -ItemType Directory -Path $logDir -Force | Out-Null

foreach ($inst in $instances) {
    if ($inst.Reused) {
        Write-Host ("[{0}] reusing {1} ({2})" -f $inst.Role, $inst.AvdName, $inst.Serial)
        continue
    }

    $emuArgs = @(
        '-avd', $inst.AvdName,
        '-port', "$($inst.ConsolePort)",
        '-gpu', $Gpu,
        '-memory', "$MemoryMb",
        '-cores', "$Cores",
        '-netdelay', 'none',
        '-netspeed', 'full',
        '-no-boot-anim',
        '-no-audio'
    )
    if ($Mode -eq 'ReadOnly') {
        $emuArgs += @('-read-only', '-no-snapshot')
    } elseif ($ColdBoot) {
        $emuArgs += @('-no-snapshot-load', '-no-snapshot-save')
    }
    if ($Headless) { $emuArgs += '-no-window' }

    $inst.Log = Join-Path $logDir ("{0}.log" -f $inst.Serial)
    $modeNote = ''
    if ($Headless) { $modeNote = ', no-window' }
    Write-Host ("[{0}] starting {1} on {2} (gpu={3}, ram={4}MB, cores={5}{6})" -f `
        $inst.Role, $inst.AvdName, $inst.Serial, $Gpu, $MemoryMb, $Cores, $modeNote)

    $inst.Process = Start-Process -FilePath $Emulator -ArgumentList $emuArgs -PassThru `
        -RedirectStandardError $inst.Log

    # Two -read-only instances of the SAME AVD race on its shared files when they
    # start together: one of them can hang before it ever opens its console port.
    # Clone mode uses one AVD per user, so there it is safe to start in parallel.
    if ($Mode -eq 'ReadOnly') {
        $inst.BootChecked = $true
        if (-not (Wait-InstanceBoot -Instance $inst -TimeoutSec $BootTimeoutSec)) {
            Write-Warning ("[{0}] {1} did not finish booting in {2}s. Log: {3}" -f `
                $inst.Role, $inst.Serial, $BootTimeoutSec, $inst.Log)
        }
    }
}

# -------------------------------------------------------------- wait boot ---

foreach ($inst in $instances) {
    if ($inst.Reused -or $inst.BootChecked) { continue }
    $inst.BootChecked = $true
    if (-not (Wait-InstanceBoot -Instance $inst -TimeoutSec $BootTimeoutSec)) {
        Write-Warning ("[{0}] {1} did not finish booting in {2}s. Log: {3}" -f `
            $inst.Role, $inst.Serial, $BootTimeoutSec, $inst.Log)
    }
}

# ------------------------------------------------ per-instance finishing ---

foreach ($inst in $instances) {
    if ((Invoke-AdbShell -Serial $inst.Serial -Command 'getprop sys.boot_completed') -ne '1') { continue }

    # CPU savers: no animations, screen never sleeps.
    foreach ($setting in @('window_animation_scale', 'transition_animation_scale', 'animator_duration_scale')) {
        Invoke-AdbShell -Serial $inst.Serial -Command "settings put global $setting 0" | Out-Null
    }
    Invoke-AdbShell -Serial $inst.Serial -Command 'svc power stayon true' | Out-Null
    Invoke-AdbShell -Serial $inst.Serial -Command 'settings put system screen_off_timeout 2147483647' | Out-Null

    if ($Install) {
        if (-not (Test-Path -LiteralPath $Apk)) {
            Write-Warning "APK not found: $Apk - run .\gradlew.bat :app:assembleDebug first."
        } else {
            Write-Host ("[{0}] installing {1}" -f $inst.Role, (Split-Path -Leaf $Apk))
            $out = ((Invoke-Adb -AdbArgs @('-s', $inst.Serial, 'install', '-r', '-t', $Apk)) -join "`n").Trim()
            if ($out -notmatch 'Success') { Write-Warning ("[{0}] install said: {1}" -f $inst.Role, $out) }
        }
    }

    if ($Launch) {
        Invoke-AdbShell -Serial $inst.Serial -Command "am start -n $Activity" | Out-Null
        Write-Host ("[{0}] launched {1}" -f $inst.Role, $Activity)
    }
}

# ---------------------------------------------------------------- summary ---

Write-Host ''
$instances | Format-Table -AutoSize -Property `
    Role,
    AvdName,
    Serial,
    AdbPort,
    @{ n = 'State'; e = { if ($_.Reused) { 'reused' } else { 'started' } } },
    @{ n = 'Pid'; e = { if ($_.Process) { $_.Process.Id } else { '-' } } } |
    Out-String -Width 160 | Write-Host

Write-Host 'Target one instance explicitly - a bare adb command picks the first device:' -ForegroundColor Cyan
foreach ($inst in $instances) {
    Write-Host ('  [{0}] adb -s {1} shell input text "hi%sfrom%s{0}"' -f $inst.Role, $inst.Serial)
}
$last = $instances[-1]
Write-Host ('  [{0}] cmd /c "adb -s {1} exec-out screencap -p > _docs\evidence\{0}.png"' -f $last.Role, $last.Serial) -ForegroundColor DarkGray
Write-Host ''
Write-Host 'Pin Gradle to one device (connectedDebugAndroidTest otherwise runs on ALL):' -ForegroundColor Cyan
Write-Host ("  `$env:ANDROID_SERIAL = '{0}'; .\gradlew.bat --stop; .\gradlew.bat :app:connectedDebugAndroidTest" -f $last.Serial)
Write-Host ''
Write-Host ('Emulator logs: {0}' -f $logDir) -ForegroundColor DarkGray
Write-Host 'Shut down: .\local-testing\emulators.ps1 -Stop' -ForegroundColor DarkGray
