param(
    [string]$StarsectorDir = $env:STARSECTOR_DIRECTORY
)

if ([string]::IsNullOrWhiteSpace($StarsectorDir)) {
    $StarsectorDir = "C:\Games\Starsector"
}

$repoJar = Join-Path (Split-Path -Parent $PSScriptRoot) "jars\weapon-inventory-mod.jar"
$liveJar = Join-Path $StarsectorDir "mods\Weapon Inventory Mod\jars\weapon-inventory-mod.jar"
$requiredClasses = @(
    "weaponinventorymod/gui/StockReviewPanelPlugin.class",
    "weaponinventorymod/core/GlobalWeaponMarketService.class",
    "weaponinventorymod/gui/WimGuiCampaignDialogHost.class",
    "weaponinventorymod/gui/WimGuiContentPanel.class",
    "weaponinventorymod/gui/WimGuiDialogDelegate.class",
    "weaponinventorymod/gui/WimGuiDialogOpener.class",
    "weaponinventorymod/gui/WimGuiDialogPanel.class",
    "weaponinventorymod/gui/WimGuiDialogTracker.class",
    "weaponinventorymod/gui/WimGuiHotkeyLatch.class",
    "weaponinventorymod/gui/WimGuiInputResult.class",
    "weaponinventorymod/gui/WimGuiButtonPoller`$ActionHandler.class",
    "weaponinventorymod/gui/WimGuiButtonSpecs.class",
    "weaponinventorymod/gui/WimGuiListBounds.class",
    "weaponinventorymod/gui/WimGuiListRow.class",
    "weaponinventorymod/gui/WimGuiListRowRenderer.class",
    "weaponinventorymod/gui/WimGuiModalActionRow.class",
    "weaponinventorymod/gui/WimGuiModalInput.class",
    "weaponinventorymod/gui/WimGuiModalListRenderer.class",
    "weaponinventorymod/gui/WimGuiModalListRenderer`$ExtraGapProvider.class",
    "weaponinventorymod/gui/WimGuiModalListRenderer`$ScrollRowFactory.class",
    "weaponinventorymod/gui/WimGuiModalListRenderResult.class",
    "weaponinventorymod/gui/WimGuiModalListSpec.class",
    "weaponinventorymod/gui/WimGuiModalPanelPlugin.class",
    "weaponinventorymod/gui/WimGuiNoopCoreInteractionListener.class",
    "weaponinventorymod/gui/WimGuiPendingDialog.class",
    "weaponinventorymod/gui/WimGuiScrollableListState.class",
    "weaponinventorymod/gui/WimGuiSemanticButtonFactory.class",
    "weaponinventorymod/gui/WimGuiColorDebug.class",
    "weaponinventorymod/gui/WimGuiColorDebug`$Target.class",
    "weaponinventorymod/gui/StockReviewFormat.class",
    "weaponinventorymod/gui/StockReviewColorDebugRows.class",
    "weaponinventorymod/gui/StockReviewFilter.class",
    "weaponinventorymod/gui/StockReviewFilterGroup.class",
    "weaponinventorymod/gui/StockReviewFilterListModel.class",
    "weaponinventorymod/gui/StockReviewFilters.class",
    "weaponinventorymod/gui/StockReviewReviewListModel.class",
    "weaponinventorymod/gui/StockReviewTradeGroup.class",
    "weaponinventorymod/gui/StockReviewTradeWarnings.class",
    "weaponinventorymod/core/WeaponStockSnapshotBuilder`$CostComparator.class",
    "weaponinventorymod/core/StockPurchaseService`$PurchaseSourcePriceComparator.class"
)
$forbiddenClasses = @(
    "weaponinventorymod/gui/StockReviewButtonBinding.class",
    "weaponinventorymod/gui/StockReviewDialogDelegate.class",
    "weaponinventorymod/gui/StockReviewPanelBoxPlugin.class",
    "weaponinventorymod/gui/StockReviewPurchasePreview.class",
    "weaponinventorymod/gui/StockReviewRowCells.class",
    "weaponinventorymod/gui/StockReviewRowRenderer.class",
    "weaponinventorymod/gui/StockReviewText.class",
    "weaponinventorymod/gui/WimGuiModalListLayout`$ExtraGapProvider.class",
    "weaponinventorymod/gui/WimGuiModalListGapAdapter.class",
    "weaponinventorymod/gui/WimGuiColorDebug`$ColorSetter.class",
    "weaponinventorymod/gui/WimGuiColorDebug`$1.class",
    "weaponinventorymod/core/StockDisplayMode.class",
    "weaponinventorymod/core/StockPurchaseService`$PurchaseSource`$1.class"
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
