param(
    [ValidateSet("Patch", "Restore")]
    [string]$Mode = "Patch",
    [string]$StarsectorDir = $env:STARSECTOR_DIRECTORY
)

if ([string]::IsNullOrWhiteSpace($StarsectorDir)) {
    $StarsectorDir = "C:\Games\Starsector"
}

$obfJar = Join-Path $StarsectorDir "starsector-core\starfarer_obf.jar"
$backupJar = "$obfJar.wim_backup"

if (-not (Test-Path -LiteralPath $obfJar)) {
    throw "Could not find starfarer_obf.jar at '$obfJar'. Set STARSECTOR_DIRECTORY or pass -StarsectorDir."
}

$patcherSrcDir = Join-Path $PSScriptRoot "patcher\src"
$patcherBuildDir = Join-Path $PSScriptRoot "patcher\build\classes"

New-Item -ItemType Directory -Force -Path $patcherBuildDir | Out-Null
Get-ChildItem -Path $patcherBuildDir -Recurse -File -ErrorAction SilentlyContinue | Remove-Item -Force

$sources = @(Get-ChildItem -Path $patcherSrcDir -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)
if ($sources.Count -eq 0) {
    throw "No patcher Java sources found under '$patcherSrcDir'."
}

$exports = @(
    "--add-exports", "java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED",
    "--add-exports", "java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED"
)

& javac @exports -d $patcherBuildDir $sources
if ($LASTEXITCODE -ne 0) {
    throw "Patcher compile failed with exit code $LASTEXITCODE."
}

$modeArg = $Mode.ToLowerInvariant()
& java @exports -cp $patcherBuildDir weaponinventorymod.patcher.CargoStackViewPatcher $modeArg $obfJar $backupJar
if ($LASTEXITCODE -ne 0) {
    throw "Patcher execution failed with exit code $LASTEXITCODE."
}

Write-Host "$Mode completed for $obfJar"
