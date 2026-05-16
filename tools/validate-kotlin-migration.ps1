param(
    [string]$JarPath = (Join-Path (Join-Path (Split-Path -Parent $PSScriptRoot) "jars") "weapons-procurement.jar"),
    [string]$PublicExportPath = (Join-Path (Join-Path (Split-Path -Parent $PSScriptRoot) "build") "public-export"),
    [switch]$RequireNoJava,
    [switch]$AllowPrivateBadgeJar
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$failures = New-Object System.Collections.Generic.List[string]

function Write-Gate {
    param(
        [string]$Status,
        [string]$Message
    )
    Write-Host ("{0}: {1}" -f $Status, $Message)
}

function Add-Failure {
    param([string]$Message)
    $script:failures.Add($Message)
    Write-Gate -Status "FAIL" -Message $Message
}

function Assert-File {
    param([string]$RelativePath)
    $path = Join-Path $repoRoot $RelativePath
    if (Test-Path -LiteralPath $path) {
        Write-Gate -Status "PASS" -Message "$RelativePath exists"
    } else {
        Add-Failure "$RelativePath is missing"
    }
}

function Get-ZipEntryNames {
    param([string]$Path)
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($Path)
    try {
        return @($zip.Entries | ForEach-Object { $_.FullName })
    } finally {
        $zip.Dispose()
    }
}

foreach ($file in @(
    "build.gradle.kts",
    "settings.gradle.kts",
    "gradle.properties",
    "gradlew",
    "gradlew.bat",
    "gradle/wrapper/gradle-wrapper.jar",
    "gradle/wrapper/gradle-wrapper.properties"
)) {
    Assert-File -RelativePath $file
}

$modInfoPath = Join-Path $repoRoot "mod_info.json"
if (Test-Path -LiteralPath $modInfoPath) {
    $modInfoText = Get-Content -LiteralPath $modInfoPath -Raw
    foreach ($dependencyId in @("lunalib", "lw_lazylib")) {
        if ($modInfoText.IndexOf($dependencyId, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
            Write-Gate -Status "PASS" -Message "mod_info.json declares $dependencyId"
        } else {
            Add-Failure "mod_info.json does not declare required dependency $dependencyId"
        }
    }
} else {
    Add-Failure "mod_info.json is missing"
}

$privateBadgeRoot = Join-Path $repoRoot "src/privateBadge"
if (Test-Path -LiteralPath $privateBadgeRoot) {
    Write-Gate -Status "PASS" -Message "private badge source set root exists"
} else {
    Add-Failure "private badge source set root is missing"
}

$javaSources = @(Get-ChildItem -LiteralPath (Join-Path $repoRoot "src") -Recurse -File -Filter *.java -ErrorAction SilentlyContinue)
if ($RequireNoJava) {
    if ($javaSources.Count -eq 0) {
        Write-Gate -Status "PASS" -Message "no Java source files remain"
    } else {
        Add-Failure "$($javaSources.Count) Java source file(s) remain under src"
    }
} else {
    Write-Gate -Status "SKIP" -Message "$($javaSources.Count) Java source file(s) remain; final no-Java gate not requested"
}

$kotlinSources = @(Get-ChildItem -LiteralPath (Join-Path $repoRoot "src") -Recurse -File -Filter *.kt -ErrorAction SilentlyContinue)
$projectWildcardImports = New-Object System.Collections.Generic.List[string]
foreach ($file in $kotlinSources) {
    $lines = Get-Content -LiteralPath $file.FullName
    for ($lineIndex = 0; $lineIndex -lt $lines.Count; $lineIndex++) {
        if ($lines[$lineIndex] -match '^\s*import\s+weaponsprocurement\..*\.\*\s*$') {
            $relative = [System.IO.Path]::GetFullPath($file.FullName).Substring([System.IO.Path]::GetFullPath($repoRoot).TrimEnd('\', '/').Length + 1)
            $projectWildcardImports.Add("${relative}:$($lineIndex + 1)")
        }
    }
}
if ($projectWildcardImports.Count -eq 0) {
    Write-Gate -Status "PASS" -Message "no repo-owned wildcard imports remain"
} else {
    Add-Failure "repo-owned wildcard imports remain:`n$($projectWildcardImports -join "`n")"
}

$badgeTerms = @(
    "WeaponsProcurementBadgeHelper",
    "WeaponsProcurementBadgeConfig",
    "WeaponsProcurementCountUpdater",
    "weaponsprocurement/extensions/WeaponsProcurementExtensions",
    "weaponsprocurement/internal/WeaponsProcurementBadge",
    "weaponsprocurement/internal/WeaponsProcurementCountUpdater"
)

if (Test-Path -LiteralPath $JarPath) {
    $entries = Get-ZipEntryNames -Path $JarPath
    $badgeEntries = @($entries | Where-Object {
        $entry = $_
        $badgeTerms | Where-Object { $entry.IndexOf($_, [System.StringComparison]::OrdinalIgnoreCase) -ge 0 }
    })
    if ($badgeEntries.Count -eq 0) {
        Write-Gate -Status "PASS" -Message "clean jar has no private badge classes"
    } elseif ($AllowPrivateBadgeJar) {
        Write-Gate -Status "SKIP" -Message "private badge jar entries allowed by parameter"
    } else {
        Add-Failure "clean jar contains private badge entries: $($badgeEntries -join ', ')"
    }
} else {
    Write-Gate -Status "SKIP" -Message "jar not present; build before jar boundary validation"
}

if (Test-Path -LiteralPath $PublicExportPath) {
    $publicLeaks = New-Object System.Collections.Generic.List[string]
    $scanFiles = Get-ChildItem -LiteralPath $PublicExportPath -Recurse -File |
        Where-Object { $_.Extension -in @(".java", ".kt", ".ps1", ".md", ".csv", ".json", ".yml", ".yaml", ".txt", ".kts", ".gradle", ".properties") }
    $publicTerms = @(
        "PRIVATE_BADGE",
        "privateBadge",
        "buildPrivateMod",
        "WeaponsProcurementBadgeHelper",
        "WeaponsProcurementBadgeConfig",
        "WeaponsProcurementCountUpdater",
        "wp_enable_patched_badges",
        "wp.config.patchedBadgesEnabled",
        "wp.private.patchedBadgesEnabled",
        "wp_total_",
        "tools/patcher",
        "CargoStackView",
        "bytecode"
    )
    foreach ($file in $scanFiles) {
        $text = Get-Content -LiteralPath $file.FullName -Raw
        if ($null -eq $text) {
            $text = ""
        }
        foreach ($term in $publicTerms) {
            if ($text.IndexOf($term, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
                $relative = [System.IO.Path]::GetFullPath($file.FullName).Substring([System.IO.Path]::GetFullPath($PublicExportPath).TrimEnd('\', '/').Length + 1)
                $publicLeaks.Add("$relative contains '$term'")
            }
        }
    }
    if ($publicLeaks.Count -eq 0) {
        Write-Gate -Status "PASS" -Message "public export has no private badge/build traces"
    } else {
        Add-Failure "public export leak scan failed:`n$($publicLeaks -join "`n")"
    }
} else {
    Write-Gate -Status "SKIP" -Message "public export not present; run tools/export-public.ps1 before export boundary validation"
}

if ($failures.Count -gt 0) {
    throw "Kotlin migration validation failed with $($failures.Count) failure(s)."
}

Write-Gate -Status "PASS" -Message "Kotlin migration foundation validation completed"
