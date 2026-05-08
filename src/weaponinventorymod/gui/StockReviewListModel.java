package weaponinventorymod.gui;

import com.fs.starfarer.api.ui.Alignment;
import weaponinventorymod.core.CreditFormat;
import weaponinventorymod.core.StockCategory;
import weaponinventorymod.core.StockItemType;
import weaponinventorymod.core.WeaponStockRecord;
import weaponinventorymod.core.WeaponStockSnapshot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class StockReviewListModel {
    private StockReviewListModel() {
    }

    static List<WimGuiListRow<StockReviewAction>> build(WeaponStockSnapshot snapshot,
                                          StockReviewState state,
                                          StockReviewTradeContext tradeContext) {
        List<WimGuiListRow<StockReviewAction>> rows = new ArrayList<WimGuiListRow<StockReviewAction>>();
        int displayed = 0;
        displayed += addItemType(rows, snapshot, state, tradeContext, StockItemType.WEAPON, false);
        displayed += addItemType(rows, snapshot, state, tradeContext, StockItemType.WING, true);
        if (displayed == 0) {
            rows.add(StockReviewListRow.empty("No tradeable weapons or wings were found at this market."));
        }
        return rows;
    }

    private static int addItemType(List<WimGuiListRow<StockReviewAction>> rows,
                                   WeaponStockSnapshot snapshot,
                                   StockReviewState state,
                                   StockReviewTradeContext tradeContext,
                                   StockItemType itemType,
                                   boolean topGap) {
        int count = snapshot == null ? 0 : snapshot.getCount(itemType);
        boolean expanded = state.isExpanded(itemType);
        rows.add(StockReviewListRow.filterHeading(
                WimGuiToggleHeading.countedLabel(itemType.getSectionLabel(), count, expanded),
                StockReviewAction.toggle(itemType),
                topGap,
                "Show or hide " + itemType.getSectionLabel().toLowerCase(java.util.Locale.US) + "."));
        if (!expanded) {
            return count;
        }
        int displayed = 0;
        displayed += addCategory(rows, snapshot, state, tradeContext, itemType, StockCategory.NO_STOCK, StockReviewStyle.NO_STOCK, false);
        displayed += addCategory(rows, snapshot, state, tradeContext, itemType, StockCategory.INSUFFICIENT, StockReviewStyle.INSUFFICIENT, true);
        displayed += addCategory(rows, snapshot, state, tradeContext, itemType, StockCategory.SUFFICIENT, StockReviewStyle.SUFFICIENT, true);
        return displayed;
    }

    private static int addCategory(List<WimGuiListRow<StockReviewAction>> rows,
                                   WeaponStockSnapshot snapshot,
                                   StockReviewState state,
                                   StockReviewTradeContext tradeContext,
                                   StockItemType itemType,
                                   StockCategory category,
                                   Color color,
                                   boolean topGap) {
        List<WeaponStockRecord> records = filteredRecords(
                StockReviewTradePlanner.visibleTradeableRecords(snapshot, itemType, category),
                state.getActiveFilters());
        boolean expanded = state.isExpanded(itemType, category);
        String label = WimGuiToggleHeading.label(categoryHeading(category, records, tradeContext), expanded);
        rows.add(StockReviewListRow.category(label, color, StockReviewAction.toggle(itemType, category), topGap,
                StockReviewTooltips.category(category)));
        if (!expanded) {
            return records.size();
        }
        if (StockItemType.WEAPON.equals(itemType) && StockCategory.NO_STOCK.equals(category)) {
            addWorstCaseTestRow(rows);
        }
        for (int i = 0; i < records.size(); i++) {
            addWeapon(rows, records.get(i), state, tradeContext);
        }
        return records.size();
    }

    private static void addWeapon(List<WimGuiListRow<StockReviewAction>> rows,
                                  WeaponStockRecord record,
                                  StockReviewState state,
                                  StockReviewTradeContext tradeContext) {
        boolean expanded = state.isWeaponExpanded(record.getWeaponId());
        String label = WimGuiToggleHeading.label(record.getDisplayName(), expanded);
        int planQuantity = tradeContext.netQuantityForWeapon(record.getWeaponId());
        int sellRemaining = tradeContext.negativeAdjustmentRemaining(record, Integer.MAX_VALUE);
        int transactionCost = tradeContext.transactionCostForWeapon(record.getWeaponId());
        int buyStepQuantity = tradeContext.positiveAdjustmentRemaining(record, 10);
        int sellStepQuantity = Math.min(10, sellRemaining);
        int sufficientDelta = tradeContext.deltaToSufficient(record);
        List<WimGuiRowCell<StockReviewAction>> cells = WimGuiRowCell.of(
                WimGuiRowCell.info(storageLabel(record.getStorageCount(), planQuantity),
                        StockReviewStyle.STOCK_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT,
                        Alignment.LMID, StockReviewTooltips.STORAGE),
                unitPriceCell(tradeContext.unitPriceForWeapon(record)),
                planCell(planQuantity, transactionCost),
                stepCell("-", sellStepQuantity, StockReviewStyle.SELL_BUTTON,
                        StockReviewAction.adjustPlan(record.getWeaponId(), -sellStepQuantity),
                        StockReviewTooltips.decreasePlan(sellStepQuantity)),
                WimGuiRowCell.standardAction("-1", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.SELL_BUTTON,
                        StockReviewAction.adjustPlan(record.getWeaponId(), -1), sellRemaining >= 1,
                        StockReviewTooltips.decreasePlan(1)),
                WimGuiRowCell.standardAction("+1", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, StockReviewStyle.BUY_BUTTON,
                        StockReviewAction.adjustPlan(record.getWeaponId(), 1),
                        tradeContext.positiveAdjustmentRemaining(record, 1) >= 1,
                        StockReviewTooltips.increasePlan(1)),
                stepCell("+", buyStepQuantity, StockReviewStyle.BUY_BUTTON,
                        StockReviewAction.adjustPlan(record.getWeaponId(), buyStepQuantity),
                        StockReviewTooltips.increasePlan(buyStepQuantity)),
                WimGuiRowCell.standardAction("Sufficient", StockReviewStyle.SUFFICIENT_BUTTON_WIDTH,
                        sufficientDelta < 0 ? StockReviewStyle.SELL_BUTTON : StockReviewStyle.BUY_BUTTON,
                        StockReviewAction.adjustToSufficient(record.getWeaponId(), sufficientDelta), sufficientDelta != 0,
                        StockReviewTooltips.sufficient(record)),
                WimGuiRowCell.standardAction("Reset", StockReviewStyle.RESET_BUTTON_WIDTH, StockReviewStyle.ACTION_BACKGROUND,
                        StockReviewAction.resetPlan(record.getWeaponId()), planQuantity != 0,
                        StockReviewTooltips.resetPlan()));
        rows.add(StockReviewListRow.weapon(label, cells, StockReviewAction.toggleWeapon(record.getWeaponId()),
                StockReviewTooltips.weapon(record)));
        if (!expanded) {
            return;
        }
        addWeaponData(rows, record, state);
    }

    private static void addWorstCaseTestRow(List<WimGuiListRow<StockReviewAction>> rows) {
        List<WimGuiRowCell<StockReviewAction>> cells = WimGuiRowCell.of(
                WimGuiRowCell.info("Storage: 99+", StockReviewStyle.STOCK_CELL_WIDTH,
                        StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT,
                        Alignment.LMID, StockReviewTooltips.STORAGE),
                WimGuiRowCell.info("Price: 99,999+" + CreditFormat.CREDIT_SYMBOL,
                        StockReviewStyle.PRICE_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND,
                        StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PRICE),
                WimGuiRowCell.info("Selling: 99+ [999,999+" + CreditFormat.CREDIT_SYMBOL + "]",
                        StockReviewStyle.PLAN_CELL_WIDTH, StockReviewStyle.PLAN_NEGATIVE,
                        StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PLAN),
                WimGuiRowCell.standardAction("-10", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH,
                        StockReviewStyle.SELL_BUTTON, StockReviewAction.debugNoop(), true,
                        StockReviewTooltips.decreasePlan(10)),
                WimGuiRowCell.standardAction("-1", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH,
                        StockReviewStyle.SELL_BUTTON, StockReviewAction.debugNoop(), true,
                        StockReviewTooltips.decreasePlan(1)),
                WimGuiRowCell.standardAction("+1", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH,
                        StockReviewStyle.BUY_BUTTON, StockReviewAction.debugNoop(), true,
                        StockReviewTooltips.increasePlan(1)),
                WimGuiRowCell.standardAction("+10", StockReviewStyle.TRADE_STEP_BUTTON_WIDTH,
                        StockReviewStyle.BUY_BUTTON, StockReviewAction.debugNoop(), true,
                        StockReviewTooltips.increasePlan(10)),
                WimGuiRowCell.standardAction("Sufficient", StockReviewStyle.SUFFICIENT_BUTTON_WIDTH,
                        StockReviewStyle.SELL_BUTTON, StockReviewAction.debugNoop(), true,
                        "Adjust the queued trade quantity so that your stock of this weapon just meets the sufficiency threshold for this weapon mount size (99)."),
                WimGuiRowCell.standardAction("Reset", StockReviewStyle.RESET_BUTTON_WIDTH,
                        StockReviewStyle.ACTION_BACKGROUND, StockReviewAction.debugNoop(), true,
                        StockReviewTooltips.resetPlan()));
        rows.add(StockReviewListRow.weapon("Suzuki-Clapteryon Thermal Prokector... (+)",
                cells,
                StockReviewAction.debugNoop(),
                "Worst-case row-width test sample. It does not affect trades."));
    }

    private static List<WeaponStockRecord> filteredRecords(List<WeaponStockRecord> records,
                                                           Set<StockReviewFilter> activeFilters) {
        if (records == null || records.isEmpty() || StockReviewFilters.count(activeFilters) <= 0) {
            return records;
        }
        List<WeaponStockRecord> result = new ArrayList<WeaponStockRecord>();
        for (int i = 0; i < records.size(); i++) {
            WeaponStockRecord record = records.get(i);
            if (StockReviewFilters.matches(record, activeFilters)) {
                result.add(record);
            }
        }
        return result;
    }

    private static String categoryHeading(StockCategory category,
                                          List<WeaponStockRecord> records,
                                          StockReviewTradeContext tradeContext) {
        int weaponTypes = records == null ? 0 : records.size();
        int selling = 0;
        int buying = 0;
        if (records != null && tradeContext != null) {
            for (int i = 0; i < records.size(); i++) {
                WeaponStockRecord record = records.get(i);
                selling += tradeContext.pendingSellQuantityForWeapon(record.getWeaponId());
                buying += tradeContext.pendingBuyQuantityForWeapon(record.getWeaponId());
            }
        }
        return category.getLabel()
                + " [Weapon Types: " + weaponTypes + "]"
                + "[Selling: " + Math.max(0, selling) + "]"
                + "[Buying: " + Math.max(0, buying) + "]";
    }

    static WimGuiRowCell<StockReviewAction> planCell(int planQuantity, int transactionCost) {
        String quantity = cappedCount(Math.abs(planQuantity));
        String total = cappedCredits(transactionCost, 999999);
        String label = planQuantity > 0
                ? "Buying: " + quantity + " [" + total + "]"
                : planQuantity < 0 ? "Selling: " + quantity + " [" + total + "]" : "Buying: 0 [" + StockReviewFormat.credits(0) + "]";
        Color fill = planQuantity > 0
                ? StockReviewStyle.PLAN_POSITIVE
                : planQuantity < 0 ? StockReviewStyle.PLAN_NEGATIVE : StockReviewStyle.CELL_BACKGROUND;
        return WimGuiRowCell.info(label, StockReviewStyle.PLAN_CELL_WIDTH, fill, StockReviewStyle.TEXT,
                Alignment.LMID, StockReviewTooltips.PLAN);
    }

    static String storageLabel(int ownedCount, int planQuantity) {
        if (planQuantity == 0) {
            return "Storage: " + cappedCount(ownedCount);
        }
        return "Storage: " + cappedCount(ownedCount) + " [" + signedCappedCount(planQuantity) + "]";
    }

    private static WimGuiRowCell<StockReviewAction> unitPriceCell(int unitPrice) {
        if (unitPrice == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            return WimGuiRowCell.info("Price: ?", StockReviewStyle.PRICE_CELL_WIDTH, StockReviewStyle.CELL_BACKGROUND,
                    StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PRICE);
        }
        return WimGuiRowCell.info("Price: " + cappedCredits(unitPrice, 99999), StockReviewStyle.PRICE_CELL_WIDTH,
                StockReviewStyle.CELL_BACKGROUND, StockReviewStyle.TEXT, Alignment.LMID, StockReviewTooltips.PRICE);
    }

    private static WimGuiRowCell<StockReviewAction> stepCell(String sign,
                                                            int quantity,
                                                            Color fill,
                                                            StockReviewAction action,
                                                            String tooltip) {
        boolean enabled = quantity > 1;
        String label = enabled ? sign + quantity : sign + "10";
        return WimGuiRowCell.standardAction(label, StockReviewStyle.TRADE_STEP_BUTTON_WIDTH, fill, action, enabled, tooltip);
    }

    static void addWeaponData(List<WimGuiListRow<StockReviewAction>> rows, WeaponStockRecord record, StockReviewState state) {
        addWeaponData(rows, record, state, StockReviewStyle.TRADE_ROW_RIGHT_BLOCK_WIDTH, StockReviewStyle.LIST_WIDTH);
    }

    static void addWeaponData(List<WimGuiListRow<StockReviewAction>> rows,
                              WeaponStockRecord record,
                              StockReviewState state,
                              float rightReserveWidth) {
        addWeaponData(rows, record, state, rightReserveWidth, StockReviewStyle.LIST_WIDTH);
    }

    static void addWeaponData(List<WimGuiListRow<StockReviewAction>> rows,
                              WeaponStockRecord record,
                              StockReviewState state,
                              float rightReserveWidth,
                              float listWidth) {
        boolean basicExpanded = isInfoSectionExpanded(state, record, "basic");
        rows.add(infoHeading("Basic Info", record, "basic", basicExpanded, rightReserveWidth));
        if (basicExpanded) {
            addBasicInfo(rows, record, rightReserveWidth, listWidth);
        }
        if (record.isWing()) {
            return;
        }
        boolean advancedExpanded = isInfoSectionExpanded(state, record, "advanced");
        rows.add(infoHeading("Advanced Info", record, "advanced", advancedExpanded, rightReserveWidth));
        if (advancedExpanded) {
            addAdvancedInfo(rows, record, rightReserveWidth, listWidth);
        }
    }

    private static void addBasicInfo(List<WimGuiListRow<StockReviewAction>> rows,
                                     WeaponStockRecord record,
                                     float rightReserveWidth,
                                     float listWidth) {
        addRequiredDataRow(rows, "Desired", String.valueOf(record.getDesiredCount()), rightReserveWidth, listWidth);
        if (record.isWing()) {
            addDataRow(rows, "Primary Role", record.getTypeLabel(), rightReserveWidth, listWidth);
            addRequiredDataRow(rows, "Size", "WING", rightReserveWidth, listWidth);
            addDataRow(rows, "Fighters", record.getWingFighterCountLabel(), rightReserveWidth, listWidth);
            addDataRow(rows, "OP", record.getWingOpCostLabel(), rightReserveWidth, listWidth);
            addDataRow(rows, "Range", record.getRangeLabel(), rightReserveWidth, listWidth);
            addDataRow(rows, "Refit(Sec)", record.getWingRefitTimeLabel(), rightReserveWidth, listWidth);
            return;
        }
        addDataRow(rows, "Primary Role", record.getPrimaryRoleLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Size", record.getSizeLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Type", record.getTypeLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "OP", record.getOpCostLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Range", record.getRangeLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Refire(Sec)", record.getRefireSecondsLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Damage", record.getDamageLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Damage/Sec (sustained)", record.getSustainedDamagePerSecondLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Flux/Sec (sustained)", record.getSustainedFluxPerSecondLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Flux/Damage", record.getFluxPerDamageLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "EMP", record.getEmpLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Max Ammo", record.getMaxAmmoLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Sec / Reload", record.getSecPerReloadLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Ammo Gain", record.getAmmoGainLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Accuracy", record.getAccuracyLabel(), rightReserveWidth, listWidth);
    }

    private static void addAdvancedInfo(List<WimGuiListRow<StockReviewAction>> rows,
                                        WeaponStockRecord record,
                                        float rightReserveWidth,
                                        float listWidth) {
        addDataRow(rows, "EMP/Second (sustained)", record.getSustainedEmpPerSecondLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Flux/EMP", record.getFluxPerEmpLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Beam DPS", record.getBeamDpsLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Charge Up", record.getBeamChargeUpLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Charge Down", record.getBeamChargeDownLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Burst Delay", record.getBurstDelayLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Turn Rate/Second", record.getTurnRateLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Min Spread", record.getMinSpreadLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Max Spread", record.getMaxSpreadLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Spread / Shot", record.getSpreadPerShotLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Spread Decay", record.getSpreadDecayLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Proj. Speed", record.getProjectileSpeedLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Launch Speed", record.getLaunchSpeedLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Flight Time", record.getFlightTimeLabel(), rightReserveWidth, listWidth);
        addDataRow(rows, "Guided", record.getGuidedLabel(), rightReserveWidth, listWidth);
    }

    private static WimGuiListRow<StockReviewAction> infoHeading(String label,
                                                                WeaponStockRecord record,
                                                                String section,
                                                                boolean expanded,
                                                                float rightReserveWidth) {
        return StockReviewListRow.nestedHeading(
                WimGuiToggleHeading.label(label, expanded),
                StockReviewStyle.SECTION_INDENT,
                rightReserveWidth,
                StockReviewAction.toggleWeapon(infoSectionKey(record, section)),
                false,
                "Show or hide " + label.toLowerCase(java.util.Locale.US) + " rows.");
    }

    private static boolean isInfoSectionExpanded(StockReviewState state, WeaponStockRecord record, String section) {
        return state == null || !state.isWeaponExpanded(infoSectionKey(record, section));
    }

    private static String infoSectionKey(WeaponStockRecord record, String section) {
        return record.getWeaponId() + "::info::" + section;
    }

    private static void addRequiredDataRow(List<WimGuiListRow<StockReviewAction>> rows,
                                           String label,
                                           String value,
                                           float rightReserveWidth,
                                           float listWidth) {
        rows.add(dataRow(label, value, rightReserveWidth, listWidth));
    }

    private static void addDataRow(List<WimGuiListRow<StockReviewAction>> rows,
                                   String label,
                                   String value,
                                   float rightReserveWidth,
                                   float listWidth) {
        if (isMeaningful(value)) {
            rows.add(dataRow(label, value, rightReserveWidth, listWidth));
        }
    }

    private static WimGuiListRow<StockReviewAction> dataRow(String label,
                                                            String value,
                                                            float rightReserveWidth,
                                                            float listWidth) {
        return StockReviewListRow.labelTextIndented(
                label,
                value,
                StockReviewStyle.DETAIL_INDENT,
                false,
                rightReserveWidth + StockReviewStyle.TEXT_LEFT_PAD,
                listWidth);
    }

    private static boolean isMeaningful(String value) {
        return value != null && value.trim().length() > 0 && !"?".equals(value.trim());
    }

    private static String cappedCount(int value) {
        return value >= 99 ? "99+" : String.valueOf(Math.max(0, value));
    }

    private static String signedCappedCount(int value) {
        String sign = value > 0 ? "+" : value < 0 ? "-" : "";
        int absolute = Math.abs(value);
        return sign + (absolute >= 99 ? "99+" : String.valueOf(absolute));
    }

    private static String cappedCredits(int credits, int cap) {
        if (credits == StockReviewQuoteBook.PRICE_UNAVAILABLE) {
            return "?";
        }
        int absolute = Math.abs(credits);
        if (absolute >= cap) {
            return CreditFormat.grouped(cap) + "+" + CreditFormat.CREDIT_SYMBOL;
        }
        return StockReviewFormat.credits(absolute);
    }

}
