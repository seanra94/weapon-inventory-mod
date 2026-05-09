param(
    [string]$StarsectorDir = $env:STARSECTOR_DIRECTORY
)

if ([string]::IsNullOrWhiteSpace($StarsectorDir)) {
    $StarsectorDir = "C:\Games\Starsector"
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$repoJar = Join-Path $repoRoot "jars\weapons-procurement.jar"
$liveJar = Join-Path $StarsectorDir "mods\Weapons Procurement\jars\weapons-procurement.jar"
$classValidator = Join-Path $PSScriptRoot "validate-jar-classes.ps1"

powershell -NoProfile -ExecutionPolicy Bypass -File $classValidator -JarPath $repoJar -Label "Repo"
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

powershell -NoProfile -ExecutionPolicy Bypass -File $classValidator -JarPath $liveJar -Label "Live"
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$repoHash = Get-FileHash -Algorithm SHA256 -LiteralPath $repoJar
$liveHash = Get-FileHash -Algorithm SHA256 -LiteralPath $liveJar
if ($repoHash.Hash -ne $liveHash.Hash) {
    throw "Repo/live jar hash mismatch: repo=$($repoHash.Hash) live=$($liveHash.Hash)"
}

Write-Host "Repo/live jar hash parity confirmed: $($repoHash.Hash)"
