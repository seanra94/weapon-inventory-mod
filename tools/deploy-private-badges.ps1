param(
    [string]$StarsectorDir = $env:STARSECTOR_DIRECTORY,
    [switch]$SkipCorePatchRefresh
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($StarsectorDir)) {
    throw "Set STARSECTOR_DIRECTORY or pass -StarsectorDir."
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $repoRoot "jars\weapons-procurement.jar"
$buildScript = Join-Path $repoRoot "build.ps1"
$deployScript = Join-Path $PSScriptRoot "deploy-live-mod.ps1"
$patcherScript = Join-Path $PSScriptRoot "cargo-stack-view-patcher.ps1"
$patchValidator = Join-Path $PSScriptRoot "validate-cargo-stack-view-patch.ps1"
$badgeValidator = Join-Path $PSScriptRoot "validate-total-badges.ps1"

function Get-JarEntries {
    param([string]$Path)

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($Path)
    try {
        return @($zip.Entries | ForEach-Object { $_.FullName })
    } finally {
        $zip.Dispose()
    }
}

function Assert-PrivateBadgeJar {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Private badge jar not found: $Path"
    }

    $entries = Get-JarEntries -Path $Path
    $required = @(
        "weaponsprocurement/internal/WeaponsProcurementBadgeHelper.class",
        "weaponsprocurement/internal/WeaponsProcurementBadgeHelper`$Companion.class",
        "weaponsprocurement/internal/WeaponsProcurementBadgeConfig.class",
        "weaponsprocurement/internal/WeaponsProcurementCountUpdater.class"
    )

    $missing = @($required | Where-Object { $entries -notcontains $_ })
    if ($missing.Count -gt 0) {
        throw "Jar is missing private badge bridge classes: $($missing -join ', ')"
    }
}

function Invoke-RequiredScript {
    param([string[]]$Arguments)

    & powershell @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code $LASTEXITCODE`: powershell $($Arguments -join ' ')"
    }
}

function Invoke-RequiredScriptWithOutput {
    param([string[]]$Arguments)

    $output = @(& powershell @Arguments 2>&1)
    $exitCode = $LASTEXITCODE
    $output | ForEach-Object { Write-Host $_ }
    if ($exitCode -ne 0) {
        throw "Command failed with exit code $exitCode`: powershell $($Arguments -join ' ')"
    }
    return $output
}

Invoke-RequiredScript -Arguments @(
    "-NoProfile",
    "-ExecutionPolicy", "Bypass",
    "-File", $buildScript,
    "-StarsectorDir", $StarsectorDir,
    "-PrivateBadge"
)

Assert-PrivateBadgeJar -Path $jarPath

if (-not $SkipCorePatchRefresh) {
    $obfJar = Join-Path $StarsectorDir "starsector-core\starfarer_obf.jar"
    $backupJar = "$obfJar.wp_backup"
    if (Test-Path -LiteralPath $backupJar) {
        Invoke-RequiredScript -Arguments @(
            "-NoProfile",
            "-ExecutionPolicy", "Bypass",
            "-File", $patcherScript,
            "-Mode", "Restore",
            "-StarsectorDir", $StarsectorDir
        )
    }

    Invoke-RequiredScript -Arguments @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $patcherScript,
        "-Mode", "Patch",
        "-StarsectorDir", $StarsectorDir
    )
}

Invoke-RequiredScript -Arguments @(
    "-NoProfile",
    "-ExecutionPolicy", "Bypass",
    "-File", $patchValidator,
    "-StarsectorDir", $StarsectorDir
)

$deployOutput = Invoke-RequiredScriptWithOutput -Arguments @(
    "-NoProfile",
    "-ExecutionPolicy", "Bypass",
    "-File", $deployScript,
    "-StarsectorDir", $StarsectorDir,
    "-AllowPrivateBadgeJar"
)

$deployQueued = (($deployOutput | Out-String) -match "Deploy queued:")
if ($deployQueued) {
    Write-Host "Private patched-badge build and patch validation passed; live deploy is queued until Starsector releases the target files."
} else {
    Invoke-RequiredScript -Arguments @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $badgeValidator,
        "-StarsectorDir", $StarsectorDir
    )

    Write-Host "Private patched-badge build, patch, deploy, and validation completed."
}
