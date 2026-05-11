param(
    [string[]]$Paths = @("README.md", "PACKAGING.md", "CONFIG.md", "CHANGELOG.md", "HANDOVER.md", "PLANS.md")
)

$repoRoot = Split-Path -Parent $PSScriptRoot
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
