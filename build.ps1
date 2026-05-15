param(
    [string]$StarsectorDir = $env:STARSECTOR_DIRECTORY,
    [switch]$SkipClean
)

if ([string]::IsNullOrWhiteSpace($StarsectorDir)) {
    throw "Set STARSECTOR_DIRECTORY or pass -StarsectorDir."
}

$apiJar = Join-Path $StarsectorDir "starsector-core\starfarer.api.jar"
$log4jJar = Join-Path $StarsectorDir "starsector-core\log4j-1.2.9.jar"
$lwjglJar = Join-Path $StarsectorDir "starsector-core\lwjgl.jar"
$jsonJar = Join-Path $StarsectorDir "starsector-core\json.jar"
$modsDir = Join-Path $StarsectorDir "mods"
if (-not (Test-Path -LiteralPath $apiJar)) {
    throw "Could not find starfarer.api.jar at '$apiJar'. Set STARSECTOR_DIRECTORY or pass -StarsectorDir."
}
if (-not (Test-Path -LiteralPath $log4jJar)) {
    throw "Could not find log4j-1.2.9.jar at '$log4jJar'. Set STARSECTOR_DIRECTORY or pass -StarsectorDir."
}
if (-not (Test-Path -LiteralPath $lwjglJar)) {
    throw "Could not find lwjgl.jar at '$lwjglJar'. Set STARSECTOR_DIRECTORY or pass -StarsectorDir."
}
if (-not (Test-Path -LiteralPath $jsonJar)) {
    throw "Could not find json.jar at '$jsonJar'. Set STARSECTOR_DIRECTORY or pass -StarsectorDir."
}

$lunaJar = $null
$preferredLunaJar = Join-Path $modsDir "LunaLib-2.0.5\jars\LunaLib.jar"
if (Test-Path -LiteralPath $preferredLunaJar) {
    $lunaJar = $preferredLunaJar
} else {
    $found = Get-ChildItem -Path $modsDir -Directory -Filter "LunaLib-*" -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        ForEach-Object { Join-Path $_.FullName "jars\LunaLib.jar" } |
        Where-Object { Test-Path -LiteralPath $_ } |
        Select-Object -First 1
    if ($found) {
        $lunaJar = $found
    }
}
if (-not $lunaJar) {
    throw "Could not find LunaLib.jar under '$modsDir'."
}

$srcDir = Join-Path $PSScriptRoot "src"
$classesDir = Join-Path $PSScriptRoot "build\classes"
$jarDir = Join-Path $PSScriptRoot "jars"
$jarPath = Join-Path $jarDir "weapons-procurement.jar"

New-Item -ItemType Directory -Force -Path $classesDir | Out-Null
New-Item -ItemType Directory -Force -Path $jarDir | Out-Null

if (-not $SkipClean) {
    Get-ChildItem -Path $classesDir -Recurse -File -ErrorAction SilentlyContinue | Remove-Item -Force
}

$sources = @(Get-ChildItem -Path $srcDir -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)
if ($sources.Count -eq 0) {
    throw "No Java sources found under '$srcDir'."
}

$compileClasspath = "$apiJar;$log4jJar;$lwjglJar;$jsonJar;$lunaJar"
& javac -cp $compileClasspath -d $classesDir $sources
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE."
}

$tempJarPath = Join-Path (Join-Path $PSScriptRoot "build") ("weapons-procurement." + [guid]::NewGuid().ToString("N") + ".tmp.jar")
& jar cf $tempJarPath -C $classesDir .
if ($LASTEXITCODE -ne 0) {
    throw "jar failed with exit code $LASTEXITCODE."
}

$jarBytes = [System.IO.File]::ReadAllBytes($tempJarPath)
$jarStream = [System.IO.File]::Open($jarPath, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write, [System.IO.FileShare]::Read)
try {
    $jarStream.Write($jarBytes, 0, $jarBytes.Length)
} finally {
    $jarStream.Close()
}
Remove-Item -LiteralPath $tempJarPath -Force -ErrorAction SilentlyContinue

Write-Host "Built $jarPath"
