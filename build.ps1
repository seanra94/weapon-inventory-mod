param(
    [string]$StarsectorDir = $env:STARSECTOR_DIRECTORY
)

if ([string]::IsNullOrWhiteSpace($StarsectorDir)) {
    $StarsectorDir = "C:\Games\Starsector"
}

$apiJar = Join-Path $StarsectorDir "starsector-core\starfarer.api.jar"
$log4jJar = Join-Path $StarsectorDir "starsector-core\log4j-1.2.9.jar"
$lwjglJar = Join-Path $StarsectorDir "starsector-core\lwjgl.jar"
if (-not (Test-Path -LiteralPath $apiJar)) {
    throw "Could not find starfarer.api.jar at '$apiJar'. Set STARSECTOR_DIRECTORY or pass -StarsectorDir."
}
if (-not (Test-Path -LiteralPath $log4jJar)) {
    throw "Could not find log4j-1.2.9.jar at '$log4jJar'. Set STARSECTOR_DIRECTORY or pass -StarsectorDir."
}
if (-not (Test-Path -LiteralPath $lwjglJar)) {
    throw "Could not find lwjgl.jar at '$lwjglJar'. Set STARSECTOR_DIRECTORY or pass -StarsectorDir."
}

$srcDir = Join-Path $PSScriptRoot "src"
$classesDir = Join-Path $PSScriptRoot "build\classes"
$jarDir = Join-Path $PSScriptRoot "jars"
$jarPath = Join-Path $jarDir "weapon-inventory-mod.jar"

New-Item -ItemType Directory -Force -Path $classesDir | Out-Null
New-Item -ItemType Directory -Force -Path $jarDir | Out-Null

Get-ChildItem -Path $classesDir -Recurse -File -ErrorAction SilentlyContinue | Remove-Item -Force

$sources = @(Get-ChildItem -Path $srcDir -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)
if ($sources.Count -eq 0) {
    throw "No Java sources found under '$srcDir'."
}

$compileClasspath = "$apiJar;$log4jJar;$lwjglJar"
& javac -cp $compileClasspath -d $classesDir $sources
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE."
}

if (Test-Path -LiteralPath $jarPath) {
    Remove-Item -LiteralPath $jarPath -Force
}

& jar cf $jarPath -C $classesDir .
if ($LASTEXITCODE -ne 0) {
    throw "jar failed with exit code $LASTEXITCODE."
}

Write-Host "Built $jarPath"
