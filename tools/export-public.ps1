param(
    [string]$OutputPath = (Join-Path (Join-Path (Split-Path -Parent $PSScriptRoot) "build") "public-export"),
    [switch]$NoClean
)

$repoRoot = Split-Path -Parent $PSScriptRoot
$resolvedRepoRoot = [System.IO.Path]::GetFullPath($repoRoot)
$resolvedOutput = [System.IO.Path]::GetFullPath($OutputPath)
$resolvedBuildRoot = [System.IO.Path]::GetFullPath((Join-Path $repoRoot "build"))

if ($resolvedOutput -eq $resolvedRepoRoot) {
    throw "Refusing to export public files over the private repo root."
}
if ($resolvedOutput -eq $resolvedBuildRoot) {
    throw "Refusing to export public files over the repo build root."
}
if (-not $resolvedOutput.StartsWith($resolvedBuildRoot + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to export outside the repo build directory: $resolvedOutput"
}
if (-not $NoClean -and (Test-Path -LiteralPath $resolvedOutput)) {
    Remove-Item -LiteralPath $resolvedOutput -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $resolvedOutput | Out-Null

$includeFiles = @(
    ".github/workflows/sanity.yml",
    ".gitignore",
    "build.ps1",
    "build.gradle.kts",
    "CHANGELOG.md",
    "CONFIG.md",
    "gradle.properties",
    "gradle/wrapper/gradle-wrapper.jar",
    "gradle/wrapper/gradle-wrapper.properties",
    "gradlew",
    "gradlew.bat",
    "mod_info.json",
    "PACKAGING.md",
    "README.md",
    "settings.gradle.kts",
    "data/campaign/rules.csv",
    "data/config/LunaSettings.csv",
    "data/config/weapons_procurement_market_blacklist.json",
    "data/config/weapons_procurement_stock.json",
    "tools/deploy-live-mod.ps1",
    "tools/validate-doc-links.ps1",
    "tools/validate-gui-button-style.ps1",
    "tools/validate-jar-classes.ps1",
    "tools/validate-live-gui-classes.ps1"
)

$excludeSourceSuffixes = @(
    "src/weaponsprocurement/internal/WeaponsProcurementBadgeHelper.java",
    "src/weaponsprocurement/internal/WeaponsProcurementBadgeConfig.java",
    "src/weaponsprocurement/internal/WeaponsProcurementCountUpdater.java",
    "src/weaponsprocurement/extensions/WeaponsProcurementExtensions.java",
    "src/privateBadge/kotlin/weaponsprocurement/internal/WeaponsProcurementBadgeHelper.kt",
    "src/privateBadge/kotlin/weaponsprocurement/internal/WeaponsProcurementBadgeConfig.kt",
    "src/privateBadge/kotlin/weaponsprocurement/internal/WeaponsProcurementCountUpdater.kt"
)

function Copy-RepoFile {
    param([string]$RelativePath)
    $source = Join-Path $repoRoot $RelativePath
    if (-not (Test-Path -LiteralPath $source)) {
        throw "Missing export source: $RelativePath"
    }
    $target = Join-Path $resolvedOutput $RelativePath
    $targetDir = Split-Path -Parent $target
    New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
    Copy-Item -LiteralPath $source -Destination $target -Force
}

function Get-RelativePath {
    param(
        [string]$BasePath,
        [string]$FullPath
    )
    $base = [System.IO.Path]::GetFullPath($BasePath).TrimEnd('\', '/')
    $full = [System.IO.Path]::GetFullPath($FullPath)
    if (-not $full.StartsWith($base + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Path '$full' is not under '$base'."
    }
    return $full.Substring($base.Length + 1)
}

foreach ($file in $includeFiles) {
    Copy-RepoFile -RelativePath $file
}

$publicWorkflow = Join-Path $resolvedOutput ".github/workflows/sanity.yml"
if (Test-Path -LiteralPath $publicWorkflow) {
    $workflowText = Get-Content -LiteralPath $publicWorkflow -Raw
    $workflowText = $workflowText.Replace(" -IncludePrivateDocs", "")
    $workflowText = [regex]::Replace(
        $workflowText,
        "(?ms)\r?\n      - name: Set up Java for jar inspection\r?\n        uses: actions/setup-java@v5\r?\n        with:\r?\n          distribution: temurin\r?\n          java-version: '17'\r?\n",
        "`r`n"
    )
    $workflowText = [regex]::Replace(
        $workflowText,
        "(?ms)\r?\n      - name: Validate committed jar stale classes\r?\n        shell: pwsh\r?\n        run: .*?validate-jar-classes.*?\r?\n",
        "`r`n"
    )
    $workflowText = [regex]::Replace(
        $workflowText,
        "(?ms)\r?\n      - name: Validate public export boundary\r?\n        shell: pwsh\r?\n        run: .*?export-public\.ps1.*?\r?\n",
        "`r`n"
    )
    $workflowText = [regex]::Replace(
        $workflowText,
        "(?ms)\r?\n      - name: Validate Kotlin migration boundaries\r?\n        shell: pwsh\r?\n        run: .*?validate-kotlin-migration\.ps1.*?\r?\n",
        "`r`n"
    )
    Set-Content -LiteralPath $publicWorkflow -Value $workflowText -NoNewline
}
$publicGradleBuild = Join-Path $resolvedOutput "build.gradle.kts"
if (Test-Path -LiteralPath $publicGradleBuild) {
    $gradleText = Get-Content -LiteralPath $publicGradleBuild -Raw
    $gradleText = [regex]::Replace(
        $gradleText,
        "(?ms)\r?\n?// PRIVATE_BADGE_GRADLE_START.*?// PRIVATE_BADGE_GRADLE_END\r?\n?",
        "`r`n"
    )
    $gradleText = [regex]::Replace(
        $gradleText,
        "(?m)^\s*java\.exclude\(""privateBadge/\*\*""\)\r?\n",
        ""
    )
    Set-Content -LiteralPath $publicGradleBuild -Value $gradleText -NoNewline
}
$publicBuildWrapper = Join-Path $resolvedOutput "build.ps1"
@'
param(
    [string]$StarsectorDir = $env:STARSECTOR_DIRECTORY,
    [switch]$SkipClean
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($StarsectorDir)) {
    throw "Set STARSECTOR_DIRECTORY or pass -StarsectorDir."
}

$gradleWrapper = Join-Path $PSScriptRoot "gradlew.bat"
if (-not (Test-Path -LiteralPath $gradleWrapper)) {
    throw "Missing Gradle wrapper at '$gradleWrapper'."
}

$gradleArgs = @("--no-daemon", "-PstarsectorDir=$StarsectorDir")
if (-not $SkipClean) {
    $gradleArgs += "clean"
}
$gradleArgs += "buildMod"

& $gradleWrapper @gradleArgs
if ($LASTEXITCODE -ne 0) {
    throw "Gradle build failed with exit code $LASTEXITCODE."
}

$jarPath = Join-Path $PSScriptRoot "jars\weapons-procurement.jar"
Write-Host "Built $jarPath"
'@ | Set-Content -LiteralPath $publicBuildWrapper -NoNewline
$publicDocValidator = Join-Path $resolvedOutput "tools/validate-doc-links.ps1"
@'
param(
    [string[]]$Paths = @(
        "README.md",
        "PACKAGING.md",
        "CONFIG.md",
        "CHANGELOG.md"
    )
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
'@ | Set-Content -LiteralPath $publicDocValidator -NoNewline

$srcRoot = Join-Path $repoRoot "src"
$sources = Get-ChildItem -LiteralPath $srcRoot -Recurse -File |
    Where-Object {
        $_.Extension -in @(".java", ".kt")
    }
foreach ($source in $sources) {
    $relative = (Get-RelativePath -BasePath $repoRoot -FullPath $source.FullName).Replace("\", "/")
    if ($relative.StartsWith("src/privateBadge/", [System.StringComparison]::OrdinalIgnoreCase)) {
        continue
    }
    if ($excludeSourceSuffixes -contains $relative) {
        continue
    }
    Copy-RepoFile -RelativePath $relative
}

$publicPlugin = Join-Path $resolvedOutput "src/weaponsprocurement/plugins/WeaponsProcurementModPlugin.java"
if (Test-Path -LiteralPath $publicPlugin) {
    $pluginText = Get-Content -LiteralPath $publicPlugin -Raw
    $hasBadgeUpdaterReference = $pluginText.IndexOf("WeaponsProcurementCountUpdater", [System.StringComparison]::Ordinal) -ge 0
    $hasBadgeStartMarker = $pluginText.IndexOf("PRIVATE_BADGE_START", [System.StringComparison]::Ordinal) -ge 0
    $hasBadgeEndMarker = $pluginText.IndexOf("PRIVATE_BADGE_END", [System.StringComparison]::Ordinal) -ge 0
    if ($hasBadgeUpdaterReference -and (-not $hasBadgeStartMarker -or -not $hasBadgeEndMarker)) {
        throw "Public export cannot safely strip private badge registration: marker block missing in WeaponsProcurementModPlugin.java"
    }
    $pluginText = $pluginText.Replace("import weaponsprocurement.internal.WeaponsProcurementCountUpdater;`r`n", "")
    $pluginText = $pluginText.Replace("import weaponsprocurement.internal.WeaponsProcurementCountUpdater;`n", "")
    $pluginText = [regex]::Replace(
        $pluginText,
        "(?m)^\s*private static final String BADGE_UPDATER_CLASS.*\r?\n",
        ""
    )
    $pluginText = [regex]::Replace(
        $pluginText,
        "(?ms)\r?\n        // PRIVATE_BADGE_START.*?        // PRIVATE_BADGE_END\r?\n",
        "`r`n"
    )
    $pluginText = [regex]::Replace(
        $pluginText,
        "(?ms)\r?\n    private void registerOptionalPrivateScript\(.*?\r?\n    private boolean hasScript",
        "`r`n    private boolean hasScript"
    )
    Set-Content -LiteralPath $publicPlugin -Value $pluginText -NoNewline
}

$publicPluginKt = Join-Path $resolvedOutput "src/main/kotlin/weaponsprocurement/plugins/WeaponsProcurementModPlugin.kt"
if (Test-Path -LiteralPath $publicPluginKt) {
    $pluginText = Get-Content -LiteralPath $publicPluginKt -Raw
    $hasBadgeUpdaterReference = $pluginText.IndexOf("WeaponsProcurementCountUpdater", [System.StringComparison]::Ordinal) -ge 0
    $hasBadgeStartMarker = $pluginText.IndexOf("PRIVATE_BADGE_START", [System.StringComparison]::Ordinal) -ge 0
    $hasBadgeEndMarker = $pluginText.IndexOf("PRIVATE_BADGE_END", [System.StringComparison]::Ordinal) -ge 0
    if ($hasBadgeUpdaterReference -and (-not $hasBadgeStartMarker -or -not $hasBadgeEndMarker)) {
        throw "Public export cannot safely strip private badge registration: marker block missing in WeaponsProcurementModPlugin.kt"
    }
    $pluginText = [regex]::Replace(
        $pluginText,
        "(?ms)\r?\n        // PRIVATE_BADGE_START.*?        // PRIVATE_BADGE_END\r?\n",
        "`r`n"
    )
    $pluginText = [regex]::Replace(
        $pluginText,
        "(?m)^\s*private const val BADGE_UPDATER_CLASS.*\r?\n",
        ""
    )
    $pluginText = [regex]::Replace(
        $pluginText,
        "(?ms)\r?\n    private fun registerOptionalPrivateScript\(.*?\r?\n    @Throws",
        "`r`n    @Throws"
    )
    $pluginText = [regex]::Replace(
        $pluginText,
        "(?ms)\r?\n    @Throws\(ClassNotFoundException::class\)\r?\n    private fun loadScriptClass\(.*?\r?\n    private fun hasScript",
        "`r`n    private fun hasScript"
    )
    Set-Content -LiteralPath $publicPluginKt -Value $pluginText -NoNewline
}

$leakTerms = @(
    "AGENTS.md",
    ".agent/",
    ".agent\",
    "HANDOVER.md",
    "PLANS.md",
    "LESSONS.md",
    "D:\Sean Mods",
    "C:\Games\Starsector",
    "starfarer_obf",
    "CargoStackView",
    "bytecode",
    "PRIVATE_BADGE",
    "privateBadge",
    "buildPrivateMod",
    "patched badge",
    "patched cargo-cell",
    "WeaponsProcurementBadgeHelper",
    "WeaponsProcurementBadgeConfig",
    "WeaponsProcurementCountUpdater",
    "wp_enable_patched_badges",
    "wp.config.patchedBadgesEnabled",
    "wp.private.patchedBadgesEnabled",
    "wp_total_",
    "tools/patcher",
    "validate-cargo-stack-view-patch",
    "validate-total-badges",
    "generate-total-badges"
)

$scanFiles = Get-ChildItem -LiteralPath $resolvedOutput -Recurse -File |
    Where-Object {
        $_.Extension -in @(".java", ".kt", ".ps1", ".md", ".csv", ".json", ".yml", ".yaml", ".txt", ".kts", ".gradle", ".properties")
    }
$leaks = @()
foreach ($file in $scanFiles) {
    $text = Get-Content -LiteralPath $file.FullName -Raw
    foreach ($term in $leakTerms) {
        if ($text.IndexOf($term, [System.StringComparison]::OrdinalIgnoreCase) -ge 0) {
            $relative = Get-RelativePath -BasePath $resolvedOutput -FullPath $file.FullName
            $leaks += "$relative contains '$term'"
        }
    }
}
if ($leaks.Count -gt 0) {
    throw "Public export leak scan failed:`n$($leaks -join "`n")"
}

Write-Host "Exported public Weapons Procurement source to $resolvedOutput"
