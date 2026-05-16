param(
    [Parameter(Mandatory = $true)]
    [string]$JarPath,

    [string]$Label = "Jar",

    [bool]$CheckRequired = $true
)

$requiredClasses = @(
    'weaponsprocurement/gui/StockReviewPanelPlugin.class',
    'weaponsprocurement/gui/StockReviewModeController.class',
    'weaponsprocurement/gui/StockReviewUiController.class',
    'weaponsprocurement/gui/StockReviewUiController$Host.class',
    'weaponsprocurement/gui/StockReviewTradeController.class',
    'weaponsprocurement/gui/StockReviewTradeController$Host.class',
    'weaponsprocurement/gui/StockReviewExecutionController.class',
    'weaponsprocurement/gui/StockReviewExecutionController$Host.class',
    'weaponsprocurement/gui/StockReviewExpansionState.class',
    'weaponsprocurement/gui/StockReviewFilterState.class',
    'weaponsprocurement/gui/StockReviewSourceState.class',
    'weaponsprocurement/gui/StockReviewFooterRenderer.class',
    'weaponsprocurement/gui/StockReviewTradeSummaryRenderer.class',
    'weaponsprocurement/gui/StockReviewLaunchState.class',
    'weaponsprocurement/stock/GlobalWeaponMarketService.class',
    'weaponsprocurement/stock/StockItemCargo.class',
    'weaponsprocurement/trade/StockMarketTransactionReporter.class',
    'weaponsprocurement/trade/StockPurchaseChecks.class',
    'weaponsprocurement/trade/StockPurchaseExecutor.class',
    'weaponsprocurement/trade/StockPurchaseLine.class',
    'weaponsprocurement/trade/StockPurchaseMarketSources.class',
    'weaponsprocurement/trade/StockPurchasePlan.class',
    'weaponsprocurement/trade/StockPurchaseService$PurchaseResult.class',
    'weaponsprocurement/trade/StockPurchaseService.class',
    'weaponsprocurement/trade/StockPurchaseSource.class',
    'weaponsprocurement/trade/StockPurchaseSource$PurchaseSourcePriceComparator.class',
    'weaponsprocurement/trade/StockSellTarget.class',
    'weaponsprocurement/stock/StockItemStacks.class',
    'weaponsprocurement/stock/FixerMarketObservedCatalog.class',
    'weaponsprocurement/stock/FixerMarketObservedCatalog$ObservedItem.class',
    'weaponsprocurement/config/WeaponMarketBlacklist.class',
    'weaponsprocurement/stock/StockItemType.class',
    'weaponsprocurement/stock/StockSourceMode.class',
    'weaponsprocurement/gui/WimGuiCampaignDialogHost.class',
    'weaponsprocurement/gui/WimGuiContentPanel.class',
    'weaponsprocurement/gui/WimGuiDialogDelegate.class',
    'weaponsprocurement/gui/WimGuiDialogOpener.class',
    'weaponsprocurement/gui/WimGuiDialogPanel.class',
    'weaponsprocurement/gui/WimGuiDialogTracker.class',
    'weaponsprocurement/gui/WimGuiHotkeyLatch.class',
    'weaponsprocurement/gui/WimGuiInputResult.class',
    'weaponsprocurement/gui/WimGuiButtonPoller$ActionHandler.class',
    'weaponsprocurement/gui/WimGuiButtonSpecs.class',
    'weaponsprocurement/gui/WimGuiListBounds.class',
    'weaponsprocurement/gui/WimGuiListRow.class',
    'weaponsprocurement/gui/WimGuiListRowRenderer.class',
    'weaponsprocurement/gui/WimGuiModalActionRow.class',
    'weaponsprocurement/gui/WimGuiModalInput.class',
    'weaponsprocurement/gui/WimGuiModalListRenderer.class',
    'weaponsprocurement/gui/WimGuiModalListRenderer$ExtraGapProvider.class',
    'weaponsprocurement/gui/WimGuiModalListRenderer$ScrollRowFactory.class',
    'weaponsprocurement/gui/WimGuiModalListRenderResult.class',
    'weaponsprocurement/gui/WimGuiModalListSpec.class',
    'weaponsprocurement/gui/WimGuiModalPanelPlugin.class',
    'weaponsprocurement/gui/WimGuiNoopCoreInteractionListener.class',
    'weaponsprocurement/gui/WimGuiPendingDialog.class',
    'weaponsprocurement/gui/WimGuiScrollableListState.class',
    'weaponsprocurement/gui/WimGuiSemanticButtonFactory.class',
    'weaponsprocurement/gui/WimGuiTooltip.class',
    'weaponsprocurement/gui/StockReviewTooltips.class',
    'weaponsprocurement/gui/WimGuiColorDebug.class',
    'weaponsprocurement/gui/WimGuiColorDebug$Target.class',
    'weaponsprocurement/gui/StockReviewFormat.class',
    'weaponsprocurement/gui/StockReviewColorDebugRows.class',
    'weaponsprocurement/gui/StockReviewFilter.class',
    'weaponsprocurement/gui/StockReviewFilterGroup.class',
    'weaponsprocurement/gui/StockReviewFilterListModel.class',
    'weaponsprocurement/gui/StockReviewFilters.class',
    'weaponsprocurement/gui/StockReviewItemInfoRows.class',
    'weaponsprocurement/gui/StockReviewReviewListModel.class',
    'weaponsprocurement/gui/StockReviewTradeRowCells.class',
    'weaponsprocurement/gui/StockReviewTradeGroup.class',
    'weaponsprocurement/gui/StockReviewTradeWarnings.class',
    'weaponsprocurement/gui/StockReviewPendingTrade.class',
    'weaponsprocurement/stock/WeaponStockSnapshotBuilder$CostComparator.class',
    'weaponsprocurement/lifecycle/WeaponsProcurementFixerCatalogUpdater.class',
    'weaponsprocurement/lifecycle/StockReviewHotkeyScript.class',
    'com/fs/starfarer/api/impl/campaign/rulecmd/WP_OpenDialog.class'
)

$forbiddenClasses = @(
    'weaponsprocurement/gui/StockReviewButtonBinding.class',
    'weaponsprocurement/gui/StockReviewDialogDelegate.class',
    'weaponsprocurement/gui/StockReviewPanelBoxPlugin.class',
    'weaponsprocurement/gui/StockReviewPurchasePreview.class',
    'weaponsprocurement/gui/StockReviewRowCells.class',
    'weaponsprocurement/gui/StockReviewRowRenderer.class',
    'weaponsprocurement/gui/StockReviewText.class',
    'weaponsprocurement/gui/StockReviewSection.class',
    'weaponsprocurement/gui/StockReviewPendingPurchase.class',
    'weaponsprocurement/gui/WimGuiModalListLayout$ExtraGapProvider.class',
    'weaponsprocurement/gui/WimGuiModalListGapAdapter.class',
    'weaponsprocurement/gui/WimGuiColorDebug$ColorSetter.class',
    'weaponsprocurement/gui/WimGuiColorDebug$1.class',
    'weaponsprocurement/core/StockDisplayMode.class',
    'weaponsprocurement/core/StockStatusClassifier.class',
    'weaponsprocurement/core/StockReviewConfig.class',
    'weaponsprocurement/core/WeaponMarketBlacklist.class',
    'weaponsprocurement/core/GlobalWeaponMarketService.class',
    'weaponsprocurement/core/StockItemCargo.class',
    'weaponsprocurement/core/StockItemStacks.class',
    'weaponsprocurement/core/FixerMarketObservedCatalog.class',
    'weaponsprocurement/core/StockItemType.class',
    'weaponsprocurement/core/StockSourceMode.class',
    'weaponsprocurement/core/WeaponStockSnapshotBuilder$CostComparator.class',
    'weaponsprocurement/core/StockMarketTransactionReporter.class',
    'weaponsprocurement/core/StockPurchaseChecks.class',
    'weaponsprocurement/core/StockPurchaseExecutor.class',
    'weaponsprocurement/core/StockPurchaseLine.class',
    'weaponsprocurement/core/StockPurchaseMarketSources.class',
    'weaponsprocurement/core/StockPurchasePlan.class',
    'weaponsprocurement/core/StockPurchaseService.class',
    'weaponsprocurement/core/StockPurchaseSource.class',
    'weaponsprocurement/core/StockSellTarget.class',
    'weaponsprocurement/core/TradeMoney.class',
    'weaponsprocurement/core/CreditFormat.class',
    'weaponsprocurement/gui/StockReviewHotkeyScript.class',
    'weaponsprocurement/internal/WeaponsProcurementConfig.class',
    'weaponsprocurement/internal/WeaponsProcurementFixerCatalogUpdater.class',
    'weaponsprocurement/core/StockPurchaseService$PurchaseLine.class',
    'weaponsprocurement/core/StockPurchaseService$PurchasePlan.class',
    'weaponsprocurement/core/StockPurchaseService$PurchaseSource.class',
    'weaponsprocurement/core/StockPurchaseService$PurchaseSourcePriceComparator.class',
    'weaponsprocurement/core/StockPurchaseService$SellTarget.class',
    'weaponsprocurement/core/StockPurchaseService$PurchaseSource$1.class'
)

if (-not (Test-Path -LiteralPath $JarPath)) {
    throw "$Label jar not found: $JarPath"
}

$entries = @(jar tf $JarPath)
if ($LASTEXITCODE -ne 0) {
    throw "Could not list $Label jar: $JarPath"
}

if ($CheckRequired) {
    $missing = @()
    foreach ($class in $requiredClasses) {
        if ($entries -notcontains $class) {
            $missing += $class
        }
    }

    if ($missing.Count -gt 0) {
        throw "$Label jar is missing required GUI classes: $($missing -join ', ')"
    }
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

Write-Host "$Label jar class validation passed."
