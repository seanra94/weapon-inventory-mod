package weaponsprocurement.core;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class StockPurchaseExecutor {
    private static final String KEY_FAIL_TRADE_STEP = "wp.debug.failTradeStep";
    private static final String FAIL_AFTER_SOURCE_REMOVAL = "after-source-removal";
    private static final String FAIL_AFTER_PLAYER_CARGO_REMOVE = "after-player-cargo-remove";
    private static final String FAIL_AFTER_PLAYER_CARGO_ADD = "after-player-cargo-add";
    private static final String FAIL_AFTER_TARGET_CARGO_ADD = "after-target-cargo-add";
    private static final String FAIL_AFTER_CREDIT_MUTATION = "after-credit-mutation";

    private StockPurchaseExecutor() {
    }

    static StockPurchaseService.PurchaseResult sellToMarket(Logger log,
                                                            MarketAPI market,
                                                            CargoAPI playerCargo,
                                                            StockSellTarget target,
                                                            StockItemType itemType,
                                                            String itemId,
                                                            int quantity) {
        int credits = target.unitPrice * quantity;
        int expectedMarketCount = StockItemCargo.itemCount(target.cargo, itemType, itemId) + quantity;
        MutationJournal journal = new MutationJournal(playerCargo, itemType, itemId);
        journal.recordCargo(playerCargo, "player cargo");
        journal.recordCargo(target.cargo, "sell target " + marketLabel(market, target.submarket));
        try {
            if (StockItemCargo.itemCount(playerCargo, itemType, itemId) < quantity) {
                return StockPurchaseService.PurchaseResult.failure("No player-cargo stock is available to sell.");
            }
            StockItemCargo.removeItem(playerCargo, itemType, itemId, quantity);
            maybeFail(FAIL_AFTER_PLAYER_CARGO_REMOVE);
            playerCargo.getCredits().add(credits);
            maybeFail(FAIL_AFTER_CREDIT_MUTATION);
            StockItemCargo.tidyCargo(playerCargo);
            StockItemCargo.addItem(target.cargo, itemType, itemId, quantity);
            maybeFail(FAIL_AFTER_TARGET_CARGO_ADD);
            StockItemCargo.tidyCargo(target.cargo);
            StockMarketTransactionReporter.reportItemTransaction(log, market, target.submarket, itemType, itemId, quantity, target.unitPrice, false);
            StockItemCargo.reconcileItemCount(target.cargo, itemType, itemId, expectedMarketCount);

            String message = "Sold " + quantity + " " + StockItemCargo.itemDisplayName(itemType, itemId)
                    + " for " + CreditFormat.creditsLong(credits) + ".";
            StockPurchaseChecks.addCampaignMessage(message);
            return StockPurchaseService.PurchaseResult.success(message, quantity, -credits);
        } catch (Throwable t) {
            return executionFailure(log, "sell to market", itemType, itemId, quantity, t, journal);
        }
    }

    static StockPurchaseService.PurchaseResult buyFromFixersMarket(Logger log,
                                                                   CargoAPI playerCargo,
                                                                   StockItemType itemType,
                                                                   String itemId,
                                                                   int quantity,
                                                                   int totalCost) {
        MutationJournal journal = new MutationJournal(playerCargo, itemType, itemId);
        journal.recordCargo(playerCargo, "player cargo");
        try {
            StockItemCargo.addItem(playerCargo, itemType, itemId, quantity);
            maybeFail(FAIL_AFTER_PLAYER_CARGO_ADD);
            playerCargo.getCredits().subtract(totalCost);
            maybeFail(FAIL_AFTER_CREDIT_MUTATION);
            StockItemCargo.tidyCargo(playerCargo);

            String message = "Bought " + quantity + " " + StockItemCargo.itemDisplayName(itemType, itemId)
                    + " from the fixer's market for " + CreditFormat.creditsLong(totalCost) + ".";
            StockPurchaseChecks.addCampaignMessage(message);
            return StockPurchaseService.PurchaseResult.success(message, quantity, totalCost);
        } catch (Throwable t) {
            return executionFailure(log, "buy from fixer's market", itemType, itemId, quantity, t, journal);
        }
    }

    static StockPurchaseService.PurchaseResult buyPlan(Logger log,
                                                       CargoAPI playerCargo,
                                                       MarketAPI fallbackMarket,
                                                       StockItemType itemType,
                                                       String itemId,
                                                       StockPurchasePlan plan,
                                                       String sourceLabel,
                                                       String operation) {
        MutationJournal journal = new MutationJournal(playerCargo, itemType, itemId);
        try {
            StockPurchaseService.PurchaseResult validation = buyPlanStillAvailable(plan, itemType, itemId);
            if (validation != null) {
                return validation;
            }
            journal.recordCargo(playerCargo, "player cargo");
            for (StockPurchaseLine line : plan.lines) {
                journal.recordCargo(line.source.cargo, "buy source " + sourceLabel(line.source, fallbackMarket));
            }
            for (StockPurchaseLine line : plan.lines) {
                StockItemCargo.removeItem(line.source.cargo, itemType, itemId, line.quantity);
                maybeFail(FAIL_AFTER_SOURCE_REMOVAL);
                StockItemCargo.tidyCargo(line.source.cargo);
                MarketAPI reportMarket = line.source.market == null ? fallbackMarket : line.source.market;
                StockMarketTransactionReporter.reportItemTransaction(log, reportMarket, line.source.submarket, itemType, itemId, line.quantity, line.source.unitPrice, true);
            }
            StockItemCargo.addItem(playerCargo, itemType, itemId, plan.totalQuantity);
            maybeFail(FAIL_AFTER_PLAYER_CARGO_ADD);
            playerCargo.getCredits().subtract(plan.totalCost);
            maybeFail(FAIL_AFTER_CREDIT_MUTATION);
            StockItemCargo.tidyCargo(playerCargo);

            String message = "Bought " + plan.totalQuantity + " " + StockItemCargo.itemDisplayName(itemType, itemId)
                    + sourceLabel + " for " + CreditFormat.creditsLong(plan.totalCost) + ".";
            StockPurchaseChecks.addCampaignMessage(message);
            return StockPurchaseService.PurchaseResult.success(message, plan.totalQuantity, plan.totalCost);
        } catch (Throwable t) {
            int totalQuantity = plan == null ? 0 : plan.totalQuantity;
            return executionFailure(log, operation, itemType, itemId, totalQuantity, t, journal);
        }
    }

    private static StockPurchaseService.PurchaseResult buyPlanStillAvailable(StockPurchasePlan plan,
                                                                             StockItemType itemType,
                                                                             String itemId) {
        if (plan == null || plan.lines == null || plan.lines.isEmpty()) {
            return StockPurchaseService.PurchaseResult.failure("No purchasable stock is available.");
        }
        for (StockPurchaseLine line : plan.lines) {
            if (line == null || line.source == null || line.source.cargo == null || line.quantity <= 0) {
                return StockPurchaseService.PurchaseResult.failure("Trade source is no longer available.");
            }
            int available = StockItemCargo.itemCount(line.source.cargo, itemType, itemId);
            if (available < line.quantity) {
                return StockPurchaseService.PurchaseResult.failure("Market stock changed before confirmation. Reopen the review and try again.");
            }
        }
        return null;
    }

    private static void maybeFail(String step) {
        String requested = System.getProperty(KEY_FAIL_TRADE_STEP, "");
        if (step.equalsIgnoreCase(requested) || "*".equals(requested)) {
            throw new RuntimeException("WP debug forced trade failure at " + step);
        }
    }

    private static StockPurchaseService.PurchaseResult executionFailure(Logger log,
                                                                        String operation,
                                                                        StockItemType itemType,
                                                                        String itemId,
                                                                        int quantity,
                                                                        Throwable t,
                                                                        MutationJournal journal) {
        String rollback = journal == null ? "rollback=none" : journal.rollback(itemType, itemId);
        log.error("WP_STOCK_REVIEW trade execution failed operation=" + operation
                + " item=" + itemType.key(itemId)
                + " quantity=" + quantity
                + " " + rollback, t);
        return StockPurchaseService.PurchaseResult.failure("Trade failed during execution. Check starsector.log.");
    }

    private static final class MutationJournal {
        private final CargoAPI playerCargo;
        private final StockItemType itemType;
        private final String itemId;
        private final float creditsBefore;
        private final List<CargoSnapshot> snapshots = new ArrayList<CargoSnapshot>();
        private final Map<CargoAPI, CargoSnapshot> snapshotsByCargo = new IdentityHashMap<CargoAPI, CargoSnapshot>();

        MutationJournal(CargoAPI playerCargo, StockItemType itemType, String itemId) {
            this.playerCargo = playerCargo;
            this.itemType = itemType;
            this.itemId = itemId;
            this.creditsBefore = playerCargo == null ? 0f : playerCargo.getCredits().get();
        }

        void recordCargo(CargoAPI cargo, String label) {
            if (cargo == null || snapshotsByCargo.containsKey(cargo)) {
                return;
            }
            CargoSnapshot snapshot = new CargoSnapshot(cargo, itemType, itemId, label);
            snapshotsByCargo.put(cargo, snapshot);
            snapshots.add(snapshot);
        }

        String rollback(StockItemType itemType, String itemId) {
            int restored = 0;
            int failed = 0;
            for (int i = 0; i < snapshots.size(); i++) {
                CargoSnapshot snapshot = snapshots.get(i);
                try {
                    snapshot.itemCountAtFailure = StockItemCargo.itemCount(snapshot.cargo, itemType, itemId);
                    StockItemCargo.reconcileItemCount(snapshot.cargo, itemType, itemId, snapshot.itemCountBefore);
                    snapshot.itemCountAfterRollback = StockItemCargo.itemCount(snapshot.cargo, itemType, itemId);
                    restored++;
                } catch (Throwable ignored) {
                    failed++;
                }
            }
            boolean creditsRestored = false;
            try {
                if (playerCargo != null) {
                    playerCargo.getCredits().set(creditsBefore);
                    creditsRestored = true;
                }
            } catch (Throwable ignored) {
            }
            return "rollback=attempted restoredCargos=" + restored
                    + " failedCargos=" + failed
                    + " creditsRestored=" + creditsRestored
                    + " touched=" + describe();
        }

        private String describe() {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < snapshots.size(); i++) {
                if (i > 0) {
                    result.append(",");
                }
                CargoSnapshot snapshot = snapshots.get(i);
                result.append(snapshot.itemCountBefore)
                        .append("[")
                        .append(snapshot.label)
                        .append("]")
                        .append("->")
                        .append(snapshot.itemCountAtFailure < 0 ? "?" : Integer.toString(snapshot.itemCountAtFailure))
                        .append("->")
                        .append(snapshot.itemCountAfterRollback < 0 ? "?" : Integer.toString(snapshot.itemCountAfterRollback));
            }
            return result.toString();
        }
    }

    private static final class CargoSnapshot {
        private final CargoAPI cargo;
        private final String label;
        private final StockItemType itemType;
        private final String itemId;
        private final int itemCountBefore;
        private int itemCountAtFailure = -1;
        private int itemCountAfterRollback = -1;

        CargoSnapshot(CargoAPI cargo, StockItemType itemType, String itemId, String label) {
            this.cargo = cargo;
            this.label = label == null ? "unknown cargo" : label;
            this.itemType = itemType;
            this.itemId = itemId;
            this.itemCountBefore = StockItemCargo.itemCount(cargo, itemType, itemId);
        }
    }

    private static String sourceLabel(StockPurchaseSource source, MarketAPI fallbackMarket) {
        if (source == null) {
            return "unknown source";
        }
        MarketAPI market = source.market == null ? fallbackMarket : source.market;
        return marketLabel(market, source.submarket);
    }

    private static String marketLabel(MarketAPI market, com.fs.starfarer.api.campaign.econ.SubmarketAPI submarket) {
        StringBuilder result = new StringBuilder();
        if (market == null) {
            result.append("unknown market");
        } else {
            result.append(safe(market.getName())).append("/").append(safe(market.getId()));
        }
        result.append(" ");
        if (submarket == null) {
            result.append("unknown submarket");
        } else {
            result.append(safe(submarket.getNameOneLine())).append("/").append(safe(submarket.getSpecId()));
        }
        return result.toString();
    }

    private static String safe(String value) {
        return value == null || value.length() == 0 ? "?" : value;
    }
}
