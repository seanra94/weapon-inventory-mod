param(
    [string]$RepoRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$DeployRoot = "C:\Games\Starsector\mods\Weapon Inventory Mod"
)

Add-Type -AssemblyName System.Drawing

$required = @(
    "wim_total_red_0.png",
    "wim_total_yellow_1.png",
    "wim_total_yellow_6.png",
    "wim_total_green_10.png",
    "wim_total_green_99plus.png",
    "wim_total_err.png"
)

function Test-BadgeSet {
    param(
        [string]$BasePath,
        [string]$Label
    )

    $uiPath = Join-Path $BasePath "graphics\\ui"
    if (-not (Test-Path -LiteralPath $uiPath)) {
        throw "$Label missing graphics\\ui at '$uiPath'"
    }

    foreach ($file in $required) {
        $path = Join-Path $uiPath $file
        if (-not (Test-Path -LiteralPath $path)) {
            throw "$Label missing file '$path'"
        }
        $item = Get-Item -LiteralPath $path
        if ($item.Length -le 0) {
            throw "$Label file has zero bytes '$path'"
        }

        $img = [System.Drawing.Image]::FromFile($path)
        try {
            if ($img.Width -ne 30 -or $img.Height -ne 18) {
                throw "$Label unexpected size for '$file': $($img.Width)x$($img.Height), expected 30x18"
            }
        } finally {
            $img.Dispose()
        }
    }
}

function Test-JsonFilesNoBom {
    param(
        [string]$BasePath,
        [string]$Label
    )

    $jsonFiles = Get-ChildItem -LiteralPath $BasePath -Recurse -File -Filter *.json
    foreach ($file in $jsonFiles) {
        $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
        if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
            throw "$Label JSON has UTF-8 BOM: '$($file.FullName)'"
        }
    }
}

function Test-StockReviewConfig {
    param(
        [string]$BasePath,
        [string]$Label
    )

    $configPath = Join-Path $BasePath "data\\config\\weapon_inventory_stock.json"
    if (-not (Test-Path -LiteralPath $configPath)) {
        throw "$Label missing stock review config '$configPath'"
    }

    $raw = Get-Content -LiteralPath $configPath -Raw
    try {
        $json = $raw | ConvertFrom-Json
    } catch {
        throw "$Label invalid stock review JSON '$configPath': $($_.Exception.Message)"
    }

    foreach ($section in @("display", "sources", "desiredDefaults", "perWeapon")) {
        if (-not $json.PSObject.Properties.Name.Contains($section)) {
            throw "$Label stock review config missing '$section'"
        }
    }
    if (-not $json.display.PSObject.Properties.Name.Contains("defaultMode")) {
        throw "$Label stock review config missing display.defaultMode"
    }
    if (-not $json.display.PSObject.Properties.Name.Contains("defaultSort")) {
        throw "$Label stock review config missing display.defaultSort"
    }
    foreach ($field in @("includeCurrentMarketStorage", "includeBlackMarket")) {
        if (-not $json.sources.PSObject.Properties.Name.Contains($field)) {
            throw "$Label stock review config missing sources.$field"
        }
    }
    foreach ($field in @("smallWeapon", "mediumWeapon", "largeWeapon")) {
        if (-not $json.desiredDefaults.PSObject.Properties.Name.Contains($field)) {
            throw "$Label stock review config missing desiredDefaults.$field"
        }
        $value = [int]$json.desiredDefaults.$field
        if ($value -lt 0 -or $value -gt 999) {
            throw "$Label stock review config desiredDefaults.$field out of range: $value"
        }
    }
}

Test-BadgeSet -BasePath $RepoRoot -Label "SOURCE"
Test-BadgeSet -BasePath $DeployRoot -Label "DEPLOY"
Test-JsonFilesNoBom -BasePath $RepoRoot -Label "SOURCE"
Test-JsonFilesNoBom -BasePath $DeployRoot -Label "DEPLOY"
Test-StockReviewConfig -BasePath $RepoRoot -Label "SOURCE"
Test-StockReviewConfig -BasePath $DeployRoot -Label "DEPLOY"

Write-Host "Total badge, stock config, and JSON BOM validation passed for source and deploy paths."
