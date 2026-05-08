param(
    [string]$StarsectorDir = $env:STARSECTOR_DIRECTORY
)

if ([string]::IsNullOrWhiteSpace($StarsectorDir)) {
    $StarsectorDir = "C:\Games\Starsector"
}

$repoJar = Join-Path (Split-Path -Parent $PSScriptRoot) "jars\weapons-procurement.jar"
$liveJar = Join-Path $StarsectorDir "mods\Weapons Procurement\jars\weapons-procurement.jar"
$requiredClasses = @(
    "weaponsprocurement/gui/StockReviewPanelPlugin.class",
    "weaponsprocurement/gui/StockReviewModeController.class",
    "weaponsprocurement/gui/StockReviewUiController.class",
    "weaponsprocurement/gui/StockReviewUiController`$Host.class",
    "weaponsprocurement/gui/StockReviewTradeController.class",
    "weaponsprocurement/gui/StockReviewTradeController`$Host.class",
    "weaponsprocurement/gui/StockReviewExecutionController.class",
    "weaponsprocurement/gui/StockReviewExecutionController`$Host.class",
    "weaponsprocurement/gui/StockReviewLaunchState.class",
    "weaponsprocurement/core/GlobalWeaponMarketService.class",
    "weaponsprocurement/core/FixerMarketObservedCatalog.class",
    "weaponsprocurement/core/FixerMarketObservedCatalog`$ObservedItem.class",
    "weaponsprocurement/core/WeaponMarketBlacklist.class",
    "weaponsprocurement/core/StockItemType.class",
    "weaponsprocurement/core/StockSourceMode.class",
    "weaponsprocurement/gui/WimGuiCampaignDialogHost.class",
    "weaponsprocurement/gui/WimGuiContentPanel.class",
    "weaponsprocurement/gui/WimGuiDialogDelegate.class",
    "weaponsprocurement/gui/WimGuiDialogOpener.class",
    "weaponsprocurement/gui/WimGuiDialogPanel.class",
    "weaponsprocurement/gui/WimGuiDialogTracker.class",
    "weaponsprocurement/gui/WimGuiHotkeyLatch.class",
    "weaponsprocurement/gui/WimGuiInputResult.class",
    "weaponsprocurement/gui/WimGuiButtonPoller`$ActionHandler.class",
    "weaponsprocurement/gui/WimGuiButtonSpecs.class",
    "weaponsprocurement/gui/WimGuiListBounds.class",
    "weaponsprocurement/gui/WimGuiListRow.class",
    "weaponsprocurement/gui/WimGuiListRowRenderer.class",
    "weaponsprocurement/gui/WimGuiModalActionRow.class",
    "weaponsprocurement/gui/WimGuiModalInput.class",
    "weaponsprocurement/gui/WimGuiModalListRenderer.class",
    "weaponsprocurement/gui/WimGuiModalListRenderer`$ExtraGapProvider.class",
    "weaponsprocurement/gui/WimGuiModalListRenderer`$ScrollRowFactory.class",
    "weaponsprocurement/gui/WimGuiModalListRenderResult.class",
    "weaponsprocurement/gui/WimGuiModalListSpec.class",
    "weaponsprocurement/gui/WimGuiModalPanelPlugin.class",
    "weaponsprocurement/gui/WimGuiNoopCoreInteractionListener.class",
    "weaponsprocurement/gui/WimGuiPendingDialog.class",
    "weaponsprocurement/gui/WimGuiScrollableListState.class",
    "weaponsprocurement/gui/WimGuiSemanticButtonFactory.class",
    "weaponsprocurement/gui/WimGuiTooltip.class",
    "weaponsprocurement/gui/StockReviewTooltips.class",
    "weaponsprocurement/gui/WimGuiColorDebug.class",
    "weaponsprocurement/gui/WimGuiColorDebug`$Target.class",
    "weaponsprocurement/gui/StockReviewFormat.class",
    "weaponsprocurement/gui/StockReviewColorDebugRows.class",
    "weaponsprocurement/gui/StockReviewFilter.class",
    "weaponsprocurement/gui/StockReviewFilterGroup.class",
    "weaponsprocurement/gui/StockReviewFilterListModel.class",
    "weaponsprocurement/gui/StockReviewFilters.class",
    "weaponsprocurement/gui/StockReviewReviewListModel.class",
    "weaponsprocurement/gui/StockReviewTradeGroup.class",
    "weaponsprocurement/gui/StockReviewTradeWarnings.class",
    "weaponsprocurement/core/WeaponStockSnapshotBuilder`$CostComparator.class",
    "weaponsprocurement/core/StockPurchaseService`$PurchaseSourcePriceComparator.class",
    "weaponsprocurement/internal/WeaponsProcurementFixerCatalogUpdater.class",
    "com/fs/starfarer/api/impl/campaign/rulecmd/WP_OpenDialog.class"
)
$forbiddenClasses = @(
    "weaponsprocurement/gui/StockReviewButtonBinding.class",
    "weaponsprocurement/gui/StockReviewDialogDelegate.class",
    "weaponsprocurement/gui/StockReviewPanelBoxPlugin.class",
    "weaponsprocurement/gui/StockReviewPurchasePreview.class",
    "weaponsprocurement/gui/StockReviewRowCells.class",
    "weaponsprocurement/gui/StockReviewRowRenderer.class",
    "weaponsprocurement/gui/StockReviewText.class",
    "weaponsprocurement/gui/StockReviewSection.class",
    "weaponsprocurement/gui/WimGuiModalListLayout`$ExtraGapProvider.class",
    "weaponsprocurement/gui/WimGuiModalListGapAdapter.class",
    "weaponsprocurement/gui/WimGuiColorDebug`$ColorSetter.class",
    "weaponsprocurement/gui/WimGuiColorDebug`$1.class",
    "weaponsprocurement/core/StockDisplayMode.class",
    "weaponsprocurement/core/StockPurchaseService`$PurchaseSource`$1.class"
)

function Test-JarClasses {
    param(
        [string]$JarPath,
        [string]$Label
    )

    if (-not (Test-Path -LiteralPath $JarPath)) {
        throw "$Label jar not found: $JarPath"
    }

    $entries = @(jar tf $JarPath)
    if ($LASTEXITCODE -ne 0) {
        throw "Could not list $Label jar: $JarPath"
    }

    $missing = @()
    foreach ($class in $requiredClasses) {
        if ($entries -notcontains $class) {
            $missing += $class
        }
    }

    if ($missing.Count -gt 0) {
        throw "$Label jar is missing required GUI classes: $($missing -join ', ')"
    }

    $stale = @()
    foreach ($class in $forbiddenClasses) {
        if ($entries -contains $class) {
            $stale += $class
        }
    }

    if ($stale.Count -gt 0) {
        throw "$Label jar contains stale GUI classes: $($stale -join ', ')"
    }

    Write-Host "$Label jar contains required GUI classes."
}

Test-JarClasses -JarPath $repoJar -Label "Repo"
Test-JarClasses -JarPath $liveJar -Label "Live"

$repoHash = Get-FileHash -Algorithm SHA256 -LiteralPath $repoJar
$liveHash = Get-FileHash -Algorithm SHA256 -LiteralPath $liveJar
if ($repoHash.Hash -ne $liveHash.Hash) {
    throw "Repo/live jar hash mismatch: repo=$($repoHash.Hash) live=$($liveHash.Hash)"
}

Write-Host "Repo/live jar hash parity confirmed: $($repoHash.Hash)"
