param(
    [switch]$IncludePrivateDocs,
    [string[]]$Paths = @(
        "README.md",
        "PACKAGING.md",
        "CONFIG.md",
        "CHANGELOG.md"
    )
)

$repoRoot = Split-Path -Parent $PSScriptRoot
if ($IncludePrivateDocs) {
    $Paths += @(
        "AGENTS.md",
        "HANDOVER.md",
        "PLANS.md",
        ".agent/INDEX.md",
        ".agent/BRIEF.md",
        ".agent/PUBLIC_RELEASE.md",
        ".agent/archive/INDEX.md",
        ".agent/archive/deep-dives/starsector-ui.md",
        ".agent/archive/deep-dives/trade-and-sources.md",
        ".agent/archive/deep-dives/patched-badges.md",
        ".agent/archive/deep-dives/runtime-validation.md",
        ".agent/archive/history/2026-05-gui-framework-migration.md",
        ".agent/archive/history/2026-05-trade-source-remediation.md",
        ".agent/archive/history/2026-05-product-and-validation-history.md"
    )
}
$badLinks = @()
$missingFiles = @()

foreach ($path in $Paths) {
    $fullPath = Join-Path $repoRoot $path
    if (-not (Test-Path -LiteralPath $fullPath)) {
        $missingFiles += $path
        continue
    }

    $content = Get-Content -LiteralPath $fullPath -Raw
    if ($content -match '\]\((?:/[A-Za-z]:|[A-Za-z]:\\|file://)') {
        $badLinks += $path
    }
}

if ($missingFiles.Count -gt 0) {
    throw "Documentation files missing: $($missingFiles -join ', ')"
}

if ($badLinks.Count -gt 0) {
    throw "Documentation contains local filesystem links: $($badLinks -join ', ')"
}

Write-Host "Documentation link validation passed."
