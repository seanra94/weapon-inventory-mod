package weaponsprocurement.gui;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.apache.log4j.Logger;
import weaponsprocurement.core.StockPurchaseService;
import weaponsprocurement.core.StockSourceMode;
import weaponsprocurement.core.SubmarketWeaponStock;
import weaponsprocurement.core.WeaponStockRecord;
import weaponsprocurement.core.WeaponStockSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class StockReviewExecutionController {
    interface Host {
        WeaponStockSnapshot snapshot();

        SectorAPI sector();

        MarketAPI market();

        void updateTradeWarning(String explicitWarning);

        void rebuildSnapshot();

        void requestContentRebuild();

        void exitReviewMode();

        void requestReopen(boolean review);

        void reopen(boolean review);

        void requestClose();

        void refreshVanillaCargoScreen();

        void postMessage(String message);
    }

    private static final Logger LOG = Logger.getLogger(StockReviewExecutionController.class);

    private final StockReviewState state;
    private final StockReviewPendingTrades pendingTrades;
    private final StockPurchaseService purchaseService;
    private final Host host;

    StockReviewExecutionController(StockReviewState state,
                                   StockReviewPendingTrades pendingTrades,
                                   StockPurchaseService purchaseService,
                                   Host host) {
        this.state = state;
        this.pendingTrades = pendingTrades;
        this.purchaseService = purchaseService;
        this.host = host;
    }

    void confirmPendingTrades() {
        if (pendingTrades.isEmpty()) {
            host.reopen(false);
            return;
        }
        WeaponStockSnapshot snapshot = host.snapshot();
        StockReviewTradeContext tradeContext = new StockReviewTradeContext(snapshot, pendingTrades.asList());
        int estimatedCost = tradeContext.totalCost();
        if (estimatedCost == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            host.postMessage("Could not price every queued item. Adjust the plan and try again.");
            host.requestContentRebuild();
            return;
        }
        float credits = tradeContext.credits();
        if (estimatedCost > 0 && credits + 0.01f < estimatedCost) {
            host.postMessage("Need " + StockReviewFormat.credits(estimatedCost) + " for these trades.");
            host.updateTradeWarning(StockReviewTradeWarnings.NOT_ENOUGH_CREDITS);
            host.requestContentRebuild();
            return;
        }
        float cargoDelta = tradeContext.totalCargoSpaceDelta();
        if (cargoDelta > tradeContext.cargoSpaceLeft() + 0.01f) {
            host.postMessage("Need " + Math.round(cargoDelta) + " cargo space for these trades.");
            host.updateTradeWarning(StockReviewTradeWarnings.NO_CARGO_CAPACITY);
            host.requestContentRebuild();
            return;
        }

        SectorAPI sector = host.sector();
        MarketAPI market = host.market();
        List<StockReviewPendingTrade> executionOrder = StockReviewTradePlanner.executionOrder(pendingTrades.asList());
        StockSourceMode sourceMode = snapshot == null ? StockSourceMode.LOCAL : snapshot.getSourceMode();
        for (int i = 0; i < executionOrder.size(); i++) {
            StockReviewPendingTrade trade = executionOrder.get(i);
            StockPurchaseService.PurchaseResult result = executePendingTradeSafely(sector, market, trade, sourceMode);
            if (result == null || !result.isSuccess()) {
                if (result != null) {
                    reportPurchaseFailure(result);
                }
                pendingTrades.removeExecuted(executionOrder, i);
                host.rebuildSnapshot();
                host.requestContentRebuild();
                return;
            }
        }
        pendingTrades.clear();
        host.updateTradeWarning(null);
        host.exitReviewMode();
        host.rebuildSnapshot();
        if (StockReviewStyle.REFRESH_VANILLA_CORE_AFTER_PURCHASE) {
            host.requestReopen(false);
            host.refreshVanillaCargoScreen();
            host.requestClose();
            return;
        }
        host.reopen(false);
    }

    private StockPurchaseService.PurchaseResult executePendingTradeSafely(SectorAPI sector,
                                                                          MarketAPI market,
                                                                          StockReviewPendingTrade trade,
                                                                          StockSourceMode sourceMode) {
        try {
            return executePendingTrade(sector, market, trade, sourceMode);
        } catch (Throwable t) {
            LOG.error("WP_STOCK_REVIEW queued trade execution crashed item="
                    + (trade == null ? "null" : trade.getItemKey())
                    + " source=" + (trade == null ? "null" : trade.getSubmarketId())
                    + " quantity=" + (trade == null ? 0 : trade.getQuantity())
                    + " sourceMode=" + sourceMode, t);
            return StockPurchaseService.PurchaseResult.failure("Trade failed during execution. Check starsector.log.");
        }
    }

    private StockPurchaseService.PurchaseResult executePendingTrade(SectorAPI sector,
                                                                    MarketAPI market,
                                                                    StockReviewPendingTrade trade,
                                                                    StockSourceMode sourceMode) {
        WeaponStockSnapshot snapshot = host.snapshot();
        WeaponStockRecord record = snapshot == null ? null : snapshot.getRecord(trade.getItemKey());
        if (record == null) {
            return StockPurchaseService.PurchaseResult.failure("No queued item record is available.");
        }
        if (trade.isSell()) {
            if (sourceMode != null && sourceMode.isRemote()) {
                return purchaseService.sellItemToMarket(sector, market, record.getItemType(), record.getItemId(), -trade.getQuantity(), false);
            }
            return purchaseService.sellItemToMarket(sector, market, record.getItemType(), record.getItemId(), -trade.getQuantity(), state.isIncludeBlackMarket());
        }
        if (StockSourceMode.FIXERS.equals(sourceMode)) {
            return purchaseService.buyItemFromFixersMarket(sector, record.getItemType(), record.getItemId(), trade.getQuantity(),
                    virtualUnitPrice(trade.getItemKey()), virtualUnitCargoSpace(trade.getItemKey()));
        }
        if (StockSourceMode.SECTOR.equals(sourceMode)) {
            return purchaseService.buyItemFromSectorSources(sector, record.getItemType(), record.getItemId(), trade.getQuantity(),
                    stockSources(trade.getItemKey(), trade.getSubmarketId()));
        }
        return purchaseService.buyCheapestItem(sector, market, record.getItemType(), record.getItemId(),
                trade.getQuantity(), state.isIncludeBlackMarket());
    }

    private void reportPurchaseFailure(StockPurchaseService.PurchaseResult result) {
        host.postMessage(result.getMessage());
        LOG.info("WP_STOCK_REVIEW trade blocked: " + result.getMessage());
    }

    private List<SubmarketWeaponStock> stockSources(String itemKey, String sourceId) {
        WeaponStockSnapshot snapshot = host.snapshot();
        WeaponStockRecord record = snapshot == null ? null : snapshot.getRecord(itemKey);
        if (record == null) {
            return Collections.emptyList();
        }
        if (sourceId == null || sourceId.isEmpty()) {
            return record.getSubmarketStocks();
        }
        List<SubmarketWeaponStock> result = new ArrayList<SubmarketWeaponStock>();
        for (int i = 0; i < record.getSubmarketStocks().size(); i++) {
            SubmarketWeaponStock stock = record.getSubmarketStocks().get(i);
            if (sourceId.equals(stock.getSourceId()) || sourceId.equals(stock.getSubmarketId())) {
                result.add(stock);
            }
        }
        return result;
    }

    private int virtualUnitPrice(String itemKey) {
        WeaponStockSnapshot snapshot = host.snapshot();
        WeaponStockRecord record = snapshot == null ? null : snapshot.getRecord(itemKey);
        return record == null ? 0 : record.getCheapestPurchasableUnitPrice();
    }

    private float virtualUnitCargoSpace(String itemKey) {
        WeaponStockSnapshot snapshot = host.snapshot();
        WeaponStockRecord record = snapshot == null ? null : snapshot.getRecord(itemKey);
        if (record == null || record.getSubmarketStocks().isEmpty()) {
            return 1f;
        }
        return Math.max(1f, record.getSubmarketStocks().get(0).getUnitCargoSpace());
    }
}
