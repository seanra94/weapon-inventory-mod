param(
    [string]$StarsectorDir = $env:STARSECTOR_DIRECTORY
)

if ([string]::IsNullOrWhiteSpace($StarsectorDir)) {
    throw "Set STARSECTOR_DIRECTORY or pass -StarsectorDir."
}

$patcher = Join-Path $PSScriptRoot "cargo-stack-view-patcher.ps1"
if (-not (Test-Path -LiteralPath $patcher)) {
    throw "Could not find patcher wrapper at '$patcher'."
}

& powershell -NoProfile -ExecutionPolicy Bypass -File $patcher -Mode Verify -StarsectorDir $StarsectorDir
if ($LASTEXITCODE -ne 0) {
    throw "CargoStackView patch verification failed with exit code $LASTEXITCODE."
}
