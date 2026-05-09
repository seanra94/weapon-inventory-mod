param(
    [string]$StarsectorDir = $env:STARSECTOR_DIRECTORY
)

if ([string]::IsNullOrWhiteSpace($StarsectorDir)) {
    $StarsectorDir = "C:\Games\Starsector"
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$deployRoot = Join-Path $StarsectorDir "mods\Weapons Procurement"

if (-not (Test-Path -LiteralPath $repoRoot)) {
    throw "Repository root not found: $repoRoot"
}

New-Item -ItemType Directory -Force -Path $deployRoot | Out-Null

$items = @(
    "data",
    "graphics",
    "jars",
    "mod_info.json",
    "README.md",
    "PACKAGING.md"
)

foreach ($item in $items) {
    $source = Join-Path $repoRoot $item
    $target = Join-Path $deployRoot $item
    if (-not (Test-Path -LiteralPath $source)) {
        throw "Deploy source item not found: $source"
    }

    $sourceItem = Get-Item -LiteralPath $source
    if ($sourceItem.PSIsContainer) {
        New-Item -ItemType Directory -Force -Path $target | Out-Null
        Copy-Item -Path (Join-Path $source "*") -Destination $target -Recurse -Force
    } else {
        Copy-Item -LiteralPath $source -Destination $target -Force
    }
}

Write-Host "Deployed Weapons Procurement clean package files to $deployRoot"
