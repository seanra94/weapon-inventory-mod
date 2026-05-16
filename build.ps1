param(
    [string]$StarsectorDir = $env:STARSECTOR_DIRECTORY,
    [switch]$SkipClean,
    [switch]$PrivateBadge
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($StarsectorDir)) {
    throw "Set STARSECTOR_DIRECTORY or pass -StarsectorDir."
}

$gradleWrapper = Join-Path $PSScriptRoot "gradlew.bat"
if (-not (Test-Path -LiteralPath $gradleWrapper)) {
    throw "Missing Gradle wrapper at '$gradleWrapper'."
}

$taskName = if ($PrivateBadge) { "buildPrivateMod" } else { "buildMod" }
$gradleArgs = @("--no-daemon", "-PstarsectorDir=$StarsectorDir")
if (-not $SkipClean) {
    $gradleArgs += "clean"
}
$gradleArgs += $taskName

& $gradleWrapper @gradleArgs
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed with exit code $LASTEXITCODE."
}

$jarPath = Join-Path $PSScriptRoot "jars\weapons-procurement.jar"
Write-Host "Built $jarPath"
