param(
    [string]$LogPath = "",
    [string[]]$ExpectHull = @(),
    [switch]$AllowMissingTargets,
    [switch]$All
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot

function Write-Gate {
    param(
        [string]$Status,
        [string]$Message
    )
    Write-Host ("{0}: {1}" -f $Status, $Message)
}

function Resolve-DefaultLogPath {
    $candidates = @(
        (Join-Path (Join-Path $repoRoot "logs") "starsector.log"),
        "C:\Games\Starsector\starsector-core\starsector.log",
        (Join-Path $repoRoot "starsector.log")
    )
    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }
    return ""
}

function Get-Field {
    param(
        [string]$Line,
        [string]$Name
    )
    $pattern = "(?:^|\s)" + [Regex]::Escape($Name) + "=(`"[^`"]*`"|\S+)"
    $match = [Regex]::Match($Line, $pattern)
    if (-not $match.Success) {
        return $null
    }
    $value = $match.Groups[1].Value
    if ($value.StartsWith('"') -and $value.EndsWith('"') -and $value.Length -ge 2) {
        return $value.Substring(1, $value.Length - 2)
    }
    return $value
}

function To-Int {
    param(
        [object]$Value,
        [int]$Default = 0
    )
    if ($null -eq $Value) {
        return $Default
    }
    $parsed = 0
    if ([int]::TryParse([string]$Value, [ref]$parsed)) {
        return $parsed
    }
    return $Default
}

function Latest-DiagnosticWindow {
    param([string[]]$Lines)
    if ($All -or $Lines.Count -eq 0) {
        return $Lines
    }
    for ($index = $Lines.Count - 1; $index -ge 0; $index--) {
        if ($Lines[$index].Contains("WP_SHIP_CATALOG_DIAG PASS summary") -or
            $Lines[$index].Contains("WP_SHIP_CATALOG_DIAG SKIP ")) {
            return @($Lines[$index..($Lines.Count - 1)])
        }
    }
    return $Lines
}

if ([string]::IsNullOrWhiteSpace($LogPath)) {
    $LogPath = Resolve-DefaultLogPath
}

if ([string]::IsNullOrWhiteSpace($LogPath) -or -not (Test-Path -LiteralPath $LogPath)) {
    Write-Gate -Status "SKIP" -Message "No log file found. Pass -LogPath or run Starsector with wp.debug.shipCatalog enabled."
    exit 0
}

$rawLines = @(Get-Content -LiteralPath $LogPath | Where-Object { $_.Contains("WP_SHIP_CATALOG_DIAG") })
if ($rawLines.Count -eq 0) {
    Write-Gate -Status "SKIP" -Message "No WP_SHIP_CATALOG_DIAG lines found in $LogPath"
    Write-Host "INFO: Start Starsector with JVM property wp.debug.shipCatalog=summary, top, all, or a hull-id list."
    exit 0
}

$lines = @(Latest-DiagnosticWindow -Lines $rawLines)
$summaryLines = @($lines | Where-Object { $_.Contains("WP_SHIP_CATALOG_DIAG PASS summary") })
$skipLines = @($lines | Where-Object { $_.Contains("WP_SHIP_CATALOG_DIAG SKIP ") })
$comparisonLines = @($lines | Where-Object { $_.Contains("WP_SHIP_CATALOG_DIAG observedComparison") })
$candidateLines = @($lines | Where-Object {
    $_.Contains(" WP_SHIP_CATALOG_DIAG top[") -or $_.Contains(" theoretical hull=")
})
$observedTargetLines = @($lines | Where-Object { $_.Contains(" observed count=") })
$missingTargetLines = @($lines | Where-Object { $_.Contains(" status=missing") })

Write-Host "Ship catalog diagnostic analysis"
Write-Host "Log: $LogPath"
Write-Host "Scope: $(if ($All) { "all diagnostic lines" } else { "latest diagnostic window" })"
Write-Host "Lines analyzed: $($lines.Count)"

if ($skipLines.Count -gt 0 -and $summaryLines.Count -eq 0) {
    Write-Gate -Status "SKIP" -Message "Latest ship catalog diagnostic did not run against a loaded economy."
    foreach ($line in $skipLines) {
        Write-Host "INFO: $line"
    }
    exit 0
}

if ($summaryLines.Count -eq 0) {
    Write-Gate -Status "FAIL" -Message "Diagnostic lines were present but no PASS summary was found."
    exit 1
}

$summary = $summaryLines[-1]
$observedHullTypes = To-Int (Get-Field -Line $summary -Name "observedHullTypes")
$theoreticalHullTypes = To-Int (Get-Field -Line $summary -Name "theoreticalHullTypes")
$common = To-Int (Get-Field -Line $summary -Name "common")
$uncommon = To-Int (Get-Field -Line $summary -Name "uncommon")
$rare = To-Int (Get-Field -Line $summary -Name "rare")
$veryRare = To-Int (Get-Field -Line $summary -Name "veryRare")

if ($theoreticalHullTypes -gt 0) {
    Write-Gate -Status "PASS" -Message "Theoretical ship catalog produced $theoreticalHullTypes hull type(s)."
} else {
    Write-Gate -Status "FAIL" -Message "Theoretical ship catalog produced zero hull types."
}

Write-Host "Observed hull types: $observedHullTypes"
Write-Host "Rarity counts: common=$common uncommon=$uncommon rare=$rare veryRare=$veryRare"

if ($comparisonLines.Count -gt 0) {
    $comparison = $comparisonLines[-1]
    Write-Host "Observed-only sample: $(Get-Field -Line $comparison -Name "observedOnlySample")"
    Write-Host "Unsupported/custom-only observed hulls: $(To-Int (Get-Field -Line $comparison -Name "unsupportedOnly"))"
} else {
    Write-Gate -Status "SKIP" -Message "No observed/theoretical comparison line found."
}

if ($candidateLines.Count -gt 0) {
    Write-Gate -Status "PASS" -Message "$($candidateLines.Count) candidate detail line(s) were logged."
} else {
    Write-Gate -Status "SKIP" -Message "No candidate detail lines logged. Use wp.debug.shipCatalog=top/all or pass hull ids."
}

$seenHulls = New-Object System.Collections.Generic.HashSet[string] ([System.StringComparer]::OrdinalIgnoreCase)
foreach ($line in ($candidateLines + $observedTargetLines)) {
    $hull = Get-Field -Line $line -Name "hull"
    if ($null -eq $hull -and $line -match 'target=([^\s]+) observed') {
        $hull = $matches[1]
    }
    if ($null -ne $hull -and $hull.Length -gt 0) {
        [void]$seenHulls.Add($hull)
    }
}

$targetFailures = New-Object System.Collections.Generic.List[string]
foreach ($expected in $ExpectHull) {
    if ([string]::IsNullOrWhiteSpace($expected)) {
        continue
    }
    if ($seenHulls.Contains($expected)) {
        Write-Gate -Status "PASS" -Message "Expected hull '$expected' appeared in diagnostic detail."
    } elseif ($AllowMissingTargets) {
        Write-Gate -Status "SKIP" -Message "Expected hull '$expected' did not appear, but missing targets are allowed."
    } else {
        $targetFailures.Add($expected)
        Write-Gate -Status "FAIL" -Message "Expected hull '$expected' did not appear in diagnostic detail."
    }
}

if ($missingTargetLines.Count -gt 0) {
    Write-Host "Missing target lines: $($missingTargetLines.Count)"
}

if ($theoreticalHullTypes -le 0 -or $targetFailures.Count -gt 0) {
    throw "Ship catalog diagnostic analysis failed."
}

Write-Gate -Status "PASS" -Message "Ship catalog diagnostic analysis completed."
