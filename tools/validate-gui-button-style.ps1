$repoRoot = Split-Path -Parent $PSScriptRoot
$legacyGuiDir = Join-Path $repoRoot "src\weaponsprocurement\gui"
$kotlinGuiDir = Join-Path $repoRoot "src\main\kotlin\weaponsprocurement\ui"
$controlsCandidates = @(
    (Join-Path $legacyGuiDir "WimGuiControls.java"),
    (Join-Path $kotlinGuiDir "WimGuiControls.kt")
)
$controlsPath = $controlsCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1

if (-not $controlsPath) {
    throw "WimGuiControls source not found. Checked: $($controlsCandidates -join ', ')"
}

$sourceFiles = @()
foreach ($dir in @($legacyGuiDir, $kotlinGuiDir)) {
    if (Test-Path -LiteralPath $dir) {
        $sourceFiles += @(Get-ChildItem -Path $dir -Recurse -File -Include *.java,*.kt)
    }
}

$checkboxHits = @($sourceFiles | Select-String -Pattern "addAreaCheckbox|addCheckbox" -SimpleMatch)
if ($checkboxHits.Count -gt 0) {
    throw "WP GUI must not use checkbox-backed buttons/toggles. Hits:`n$($checkboxHits -join "`n")"
}

$directButtonHits = @($sourceFiles |
    Where-Object { $_.FullName -ne $controlsPath } |
    Select-String -Pattern ".addButton(" -SimpleMatch)
if ($directButtonHits.Count -gt 0) {
    throw "WP GUI buttons must route through WimGuiControls.addButton. Hits:`n$($directButtonHits -join "`n")"
}

$controlsText = Get-Content -LiteralPath $controlsPath -Raw
if ($controlsText -notmatch "dimForIdle\(idle\)") {
    throw "WimGuiControls.addButton must dim the inner button fill from the idle color."
}
if ($controlsText -notmatch "(Color|val) hover = .*colors(\?|\.)\.hover|val hover = colors\?\.hover") {
    throw "WimGuiControls.addButton must keep hover color separate from the dimmed inner idle fill."
}

Write-Host "WP GUI button style validation passed."
