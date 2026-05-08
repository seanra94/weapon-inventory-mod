$repoRoot = Split-Path -Parent $PSScriptRoot
$guiDir = Join-Path $repoRoot "src\weaponsprocurement\gui"
$controlsPath = Join-Path $guiDir "WimGuiControls.java"

if (-not (Test-Path -LiteralPath $controlsPath)) {
    throw "WimGuiControls.java not found at '$controlsPath'."
}

$javaFiles = @(Get-ChildItem -Path $guiDir -Recurse -Filter *.java)
$checkboxHits = @($javaFiles | Select-String -Pattern "addAreaCheckbox|addCheckbox" -SimpleMatch)
if ($checkboxHits.Count -gt 0) {
    throw "WP GUI must not use checkbox-backed buttons/toggles. Hits:`n$($checkboxHits -join "`n")"
}

$directButtonHits = @($javaFiles |
    Where-Object { $_.FullName -ne $controlsPath } |
    Select-String -Pattern ".addButton(" -SimpleMatch)
if ($directButtonHits.Count -gt 0) {
    throw "WP GUI buttons must route through WimGuiControls.addButton. Hits:`n$($directButtonHits -join "`n")"
}

$controlsText = Get-Content -LiteralPath $controlsPath -Raw
if ($controlsText -notmatch "dimForIdle\(idle\)") {
    throw "WimGuiControls.addButton must dim the inner button fill from the idle color."
}
if ($controlsText -notmatch "Color hover = .*colors\.hover") {
    throw "WimGuiControls.addButton must keep hover color separate from the dimmed inner idle fill."
}

Write-Host "WP GUI button style validation passed."
