param(
    [Parameter(Mandatory = $true)]
    [string]$JarPath,

    [string]$Label = "Jar",

    [bool]$CheckRequired = $true
)

$requiredClasses = @(
    'weaponsprocurement/ui/stockreview/rendering/StockReviewPanelPlugin.class',
    'weaponsprocurement/ui/stockreview/state/StockReviewModeController.class',
    'weaponsprocurement/ui/stockreview/rendering/StockReviewUiController.class',
    'weaponsprocurement/ui/stockreview/rendering/StockReviewUiController$Host.class',
    'weaponsprocurement/ui/stockreview/trade/StockReviewTradeController.class',
    'weaponsprocurement/ui/stockreview/trade/StockReviewTradeController$Host.class',
    'weaponsprocurement/ui/stockreview/trade/StockReviewExecutionController.class',
    'weaponsprocurement/ui/stockreview/trade/StockReviewExecutionController$Host.class',
    'weaponsprocurement/ui/stockreview/state/StockReviewExpansionState.class',
    'weaponsprocurement/ui/stockreview/state/StockReviewFilterState.class',
    'weaponsprocurement/ui/stockreview/state/StockReviewSourceState.class',
    'weaponsprocurement/ui/stockreview/rows/StockReviewFooterRenderer.class',
    'weaponsprocurement/ui/stockreview/rows/StockReviewTradeSummaryRenderer.class',
    'weaponsprocurement/ui/stockreview/state/StockReviewLaunchState.class',
    'weaponsprocurement/stock/market/GlobalWeaponMarketService.class',
    'weaponsprocurement/stock/item/StockItemCargo.class',
    'weaponsprocurement/trade/execution/StockMarketTransactionReporter.class',
    'weaponsprocurement/trade/execution/StockPurchaseChecks.class',
    'weaponsprocurement/trade/execution/StockPurchaseExecutor.class',
    'weaponsprocurement/trade/plan/StockPurchaseLine.class',
    'weaponsprocurement/trade/execution/StockPurchaseMarketSources.class',
    'weaponsprocurement/trade/plan/StockPurchasePlan.class',
    'weaponsprocurement/trade/execution/StockPurchaseService$PurchaseResult.class',
    'weaponsprocurement/trade/execution/StockPurchaseService.class',
    'weaponsprocurement/trade/plan/StockPurchaseSource.class',
    'weaponsprocurement/trade/plan/StockPurchaseSource$PurchaseSourcePriceComparator.class',
    'weaponsprocurement/trade/plan/StockSellTarget.class',
    'weaponsprocurement/trade/plan/TradeMoney.class',
    'weaponsprocurement/trade/quote/CreditFormat.class',
    'weaponsprocurement/stock/item/StockItemStacks.class',
    'weaponsprocurement/stock/fixer/FixerMarketObservedCatalog.class',
    'weaponsprocurement/stock/fixer/FixerMarketObservedCatalog$ObservedItem.class',
    'weaponsprocurement/config/WeaponMarketBlacklist.class',
    'weaponsprocurement/stock/item/StockItemType.class',
    'weaponsprocurement/stock/item/StockSourceMode.class',
    'weaponsprocurement/ui/WimGuiCampaignDialogHost.class',
    'weaponsprocurement/ui/WimGuiContentPanel.class',
    'weaponsprocurement/ui/WimGuiDialogDelegate.class',
    'weaponsprocurement/ui/WimGuiDialogOpener.class',
    'weaponsprocurement/ui/WimGuiDialogPanel.class',
    'weaponsprocurement/ui/WimGuiDialogTracker.class',
    'weaponsprocurement/ui/WimGuiHotkeyLatch.class',
    'weaponsprocurement/ui/WimGuiInputResult.class',
    'weaponsprocurement/ui/WimGuiButtonPoller$ActionHandler.class',
    'weaponsprocurement/ui/WimGuiButtonSpecs.class',
    'weaponsprocurement/ui/WimGuiListBounds.class',
    'weaponsprocurement/ui/WimGuiListRow.class',
    'weaponsprocurement/ui/WimGuiListRowRenderer.class',
    'weaponsprocurement/ui/WimGuiModalActionRow.class',
    'weaponsprocurement/ui/WimGuiModalInput.class',
    'weaponsprocurement/ui/WimGuiModalListRenderer.class',
    'weaponsprocurement/ui/WimGuiModalListRenderer$ExtraGapProvider.class',
    'weaponsprocurement/ui/WimGuiModalListRenderer$ScrollRowFactory.class',
    'weaponsprocurement/ui/WimGuiModalListRenderResult.class',
    'weaponsprocurement/ui/WimGuiModalListSpec.class',
    'weaponsprocurement/ui/WimGuiModalPanelPlugin.class',
    'weaponsprocurement/ui/WimGuiNoopCoreInteractionListener.class',
    'weaponsprocurement/ui/WimGuiPendingDialog.class',
    'weaponsprocurement/ui/WimGuiScrollableListState.class',
    'weaponsprocurement/ui/WimGuiSemanticButtonFactory.class',
    'weaponsprocurement/ui/WimGuiTooltip.class',
    'weaponsprocurement/ui/stockreview/tooltips/StockReviewTooltips.class',
    'weaponsprocurement/ui/WimGuiColorDebug.class',
    'weaponsprocurement/ui/WimGuiColorDebug$Target.class',
    'weaponsprocurement/ui/stockreview/rendering/StockReviewFormat.class',
    'weaponsprocurement/ui/stockreview/rows/StockReviewColorDebugRows.class',
    'weaponsprocurement/ui/stockreview/state/StockReviewFilter.class',
    'weaponsprocurement/ui/stockreview/state/StockReviewFilterGroup.class',
    'weaponsprocurement/ui/stockreview/state/StockReviewFilterListModel.class',
    'weaponsprocurement/ui/stockreview/state/StockReviewFilters.class',
    'weaponsprocurement/ui/stockreview/rows/StockReviewItemInfoRows.class',
    'weaponsprocurement/ui/stockreview/rows/StockReviewReviewListModel.class',
    'weaponsprocurement/ui/stockreview/rows/StockReviewTradeRowCells.class',
    'weaponsprocurement/ui/stockreview/trade/StockReviewTradeGroup.class',
    'weaponsprocurement/ui/stockreview/trade/StockReviewTradeWarnings.class',
    'weaponsprocurement/ui/stockreview/trade/StockReviewPendingTrade.class',
    'weaponsprocurement/stock/item/WeaponStockSnapshotBuilder$CostComparator.class',
    'weaponsprocurement/lifecycle/WeaponsProcurementFixerCatalogUpdater.class',
    'weaponsprocurement/lifecycle/StockReviewHotkeyScript.class',
    'com/fs/starfarer/api/impl/campaign/rulecmd/WP_OpenDialog.class'
)

$forbiddenClasses = @(
    'weaponsprocurement/ui/stockreview/StockReviewButtonBinding.class',
    'weaponsprocurement/ui/stockreview/StockReviewDialogDelegate.class',
    'weaponsprocurement/ui/stockreview/StockReviewPanelBoxPlugin.class',
    'weaponsprocurement/ui/stockreview/StockReviewPurchasePreview.class',
    'weaponsprocurement/ui/stockreview/StockReviewRowCells.class',
    'weaponsprocurement/ui/stockreview/StockReviewRowRenderer.class',
    'weaponsprocurement/ui/stockreview/StockReviewText.class',
    'weaponsprocurement/ui/stockreview/StockReviewSection.class',
    'weaponsprocurement/ui/stockreview/StockReviewPendingPurchase.class',
    'weaponsprocurement/ui/stockreview/StockReviewPanelPlugin.class',
    'weaponsprocurement/ui/stockreview/StockReviewModeController.class',
    'weaponsprocurement/ui/stockreview/StockReviewUiController.class',
    'weaponsprocurement/ui/stockreview/StockReviewUiController$Host.class',
    'weaponsprocurement/ui/stockreview/StockReviewTradeController.class',
    'weaponsprocurement/ui/stockreview/StockReviewTradeController$Host.class',
    'weaponsprocurement/ui/stockreview/StockReviewExecutionController.class',
    'weaponsprocurement/ui/stockreview/StockReviewExecutionController$Host.class',
    'weaponsprocurement/ui/stockreview/StockReviewExpansionState.class',
    'weaponsprocurement/ui/stockreview/StockReviewFilterState.class',
    'weaponsprocurement/ui/stockreview/StockReviewSourceState.class',
    'weaponsprocurement/ui/stockreview/StockReviewFooterRenderer.class',
    'weaponsprocurement/ui/stockreview/StockReviewTradeSummaryRenderer.class',
    'weaponsprocurement/ui/stockreview/StockReviewLaunchState.class',
    'weaponsprocurement/ui/stockreview/StockReviewTooltips.class',
    'weaponsprocurement/ui/stockreview/StockReviewFormat.class',
    'weaponsprocurement/ui/stockreview/StockReviewColorDebugRows.class',
    'weaponsprocurement/ui/stockreview/StockReviewFilter.class',
    'weaponsprocurement/ui/stockreview/StockReviewFilterGroup.class',
    'weaponsprocurement/ui/stockreview/StockReviewFilterListModel.class',
    'weaponsprocurement/ui/stockreview/StockReviewFilters.class',
    'weaponsprocurement/ui/stockreview/StockReviewItemInfoRows.class',
    'weaponsprocurement/ui/stockreview/StockReviewReviewListModel.class',
    'weaponsprocurement/ui/stockreview/StockReviewTradeRowCells.class',
    'weaponsprocurement/ui/stockreview/StockReviewTradeGroup.class',
    'weaponsprocurement/ui/stockreview/StockReviewTradeWarnings.class',
    'weaponsprocurement/ui/stockreview/StockReviewPendingTrade.class',
    'weaponsprocurement/ui/WimGuiModalListLayout$ExtraGapProvider.class',
    'weaponsprocurement/ui/WimGuiModalListGapAdapter.class',
    'weaponsprocurement/ui/WimGuiColorDebug$ColorSetter.class',
    'weaponsprocurement/ui/WimGuiColorDebug$1.class',
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
    'weaponsprocurement/stock/GlobalWeaponMarketService.class',
    'weaponsprocurement/stock/StockItemCargo.class',
    'weaponsprocurement/stock/StockItemStacks.class',
    'weaponsprocurement/stock/FixerMarketObservedCatalog.class',
    'weaponsprocurement/stock/FixerMarketObservedCatalog$ObservedItem.class',
    'weaponsprocurement/stock/StockItemType.class',
    'weaponsprocurement/stock/StockSourceMode.class',
    'weaponsprocurement/stock/WeaponStockSnapshotBuilder$CostComparator.class',
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
    'weaponsprocurement/trade/TradeMoney.class',
    'weaponsprocurement/trade/CreditFormat.class',
    'weaponsprocurement/ui/stockreview/StockReviewHotkeyScript.class',
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
