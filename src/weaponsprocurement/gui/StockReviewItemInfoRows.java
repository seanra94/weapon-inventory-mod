package weaponsprocurement.gui;

import weaponsprocurement.core.WeaponStockRecord;

import java.util.List;

final class StockReviewItemInfoRows {
    private StockReviewItemInfoRows() {
    }

    static void add(List<WimGuiListRow<StockReviewAction>> rows, WeaponStockRecord record, StockReviewState state) {
        add(rows, record, state, StockReviewStyle.TRADE_ROW_RIGHT_BLOCK_WIDTH,
                StockReviewStyle.LIST_WIDTH, StockReviewStyle.DETAIL_INDENT, StockReviewStyle.DATA_INDENT);
    }

    static void add(List<WimGuiListRow<StockReviewAction>> rows,
                    WeaponStockRecord record,
                    StockReviewState state,
                    float rightReserveWidth) {
        add(rows, record, state, rightReserveWidth,
                StockReviewStyle.LIST_WIDTH, StockReviewStyle.SECTION_INDENT, StockReviewStyle.DETAIL_INDENT);
    }

    static void add(List<WimGuiListRow<StockReviewAction>> rows,
                    WeaponStockRecord record,
                    StockReviewState state,
                    float rightReserveWidth,
                    float listWidth) {
        add(rows, record, state, rightReserveWidth, listWidth,
                StockReviewStyle.SECTION_INDENT, StockReviewStyle.DETAIL_INDENT);
    }

    static void add(List<WimGuiListRow<StockReviewAction>> rows,
                    WeaponStockRecord record,
                    StockReviewState state,
                    float rightReserveWidth,
                    float listWidth,
                    float infoIndent,
                    float dataIndent) {
        boolean basicExpanded = isInfoSectionExpanded(state, record, "basic");
        rows.add(infoHeading("Basic Info", record, "basic", basicExpanded, infoIndent, rightReserveWidth));
        if (basicExpanded) {
            addBasicInfo(rows, record, rightReserveWidth, listWidth, dataIndent);
        }
        if (record.isWing()) {
            return;
        }
        boolean advancedExpanded = isInfoSectionExpanded(state, record, "advanced");
        rows.add(infoHeading("Advanced Info", record, "advanced", advancedExpanded, infoIndent, rightReserveWidth));
        if (advancedExpanded) {
            addAdvancedInfo(rows, record, rightReserveWidth, listWidth, dataIndent);
        }
    }

    private static void addBasicInfo(List<WimGuiListRow<StockReviewAction>> rows,
                                     WeaponStockRecord record,
                                     float rightReserveWidth,
                                     float listWidth,
                                     float dataIndent) {
        addRequiredDataRow(rows, "Desired", String.valueOf(record.getDesiredCount()), rightReserveWidth, listWidth, dataIndent);
        if (record.isWing()) {
            addDataRow(rows, "Primary Role", record.getTypeLabel(), rightReserveWidth, listWidth, dataIndent);
            addRequiredDataRow(rows, "Size", "WING", rightReserveWidth, listWidth, dataIndent);
            addDataRow(rows, "Fighters", record.getWingFighterCountLabel(), rightReserveWidth, listWidth, dataIndent);
            addDataRow(rows, "OP", record.getWingOpCostLabel(), rightReserveWidth, listWidth, dataIndent);
            addDataRow(rows, "Range", record.getRangeLabel(), rightReserveWidth, listWidth, dataIndent);
            addDataRow(rows, "Refit(Sec)", record.getWingRefitTimeLabel(), rightReserveWidth, listWidth, dataIndent);
            return;
        }
        addDataRow(rows, "Primary Role", record.getPrimaryRoleLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Size", record.getSizeLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Type", record.getTypeLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "OP", record.getOpCostLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Range", record.getRangeLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Refire(Sec)", record.getRefireSecondsLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Damage", record.getDamageLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, record.hasDifferentSustainedDamagePerSecond() ? "Damage/Sec (sustained)" : "Damage/Sec",
                record.getSustainedDamagePerSecondLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, record.hasDifferentSustainedFluxPerSecond() ? "Flux/Sec (sustained)" : "Flux/Sec",
                record.getSustainedFluxPerSecondLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Flux/Damage", record.getFluxPerDamageLabel(), rightReserveWidth, listWidth, dataIndent);
        addPositiveDataRow(rows, "EMP", record.getEmpLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Max Ammo", record.getMaxAmmoLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Sec / Reload", record.getSecPerReloadLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Ammo Gain", record.getAmmoGainLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Accuracy", record.getAccuracyLabel(), rightReserveWidth, listWidth, dataIndent);
    }

    private static void addAdvancedInfo(List<WimGuiListRow<StockReviewAction>> rows,
                                        WeaponStockRecord record,
                                        float rightReserveWidth,
                                        float listWidth,
                                        float dataIndent) {
        addPositiveDataRow(rows,
                record.hasDifferentSustainedEmpPerSecond() ? "EMP/Second (sustained)" : "EMP/Second",
                record.getSustainedEmpPerSecondLabel(), rightReserveWidth, listWidth, dataIndent);
        addPositiveDataRow(rows, "Flux/EMP", record.getFluxPerEmpLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Beam DPS", record.getBeamDpsLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Charge Up", record.getBeamChargeUpLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Charge Down", record.getBeamChargeDownLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Burst Delay", record.getBurstDelayLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Turn Rate/Second", record.getTurnRateLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Min Spread", record.getMinSpreadLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Max Spread", record.getMaxSpreadLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Spread / Shot", record.getSpreadPerShotLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Spread Decay", record.getSpreadDecayLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Proj. Speed", record.getProjectileSpeedLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Launch Speed", record.getLaunchSpeedLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Flight Time", record.getFlightTimeLabel(), rightReserveWidth, listWidth, dataIndent);
        addDataRow(rows, "Guided", record.getGuidedLabel(), rightReserveWidth, listWidth, dataIndent);
    }

    private static WimGuiListRow<StockReviewAction> infoHeading(String label,
                                                                WeaponStockRecord record,
                                                                String section,
                                                                boolean expanded,
                                                                float indent,
                                                                float rightReserveWidth) {
        return StockReviewListRow.nestedHeading(
                WimGuiToggleHeading.label(label, expanded),
                indent,
                rightReserveWidth,
                StockReviewAction.toggleItem(infoSectionKey(record, section)),
                false,
                "Show or hide " + label.toLowerCase(java.util.Locale.US) + " rows.");
    }

    private static boolean isInfoSectionExpanded(StockReviewState state, WeaponStockRecord record, String section) {
        return state == null || !state.isItemExpanded(infoSectionKey(record, section));
    }

    private static String infoSectionKey(WeaponStockRecord record, String section) {
        return record.getItemKey() + "::info::" + section;
    }

    private static void addRequiredDataRow(List<WimGuiListRow<StockReviewAction>> rows,
                                           String label,
                                           String value,
                                           float rightReserveWidth,
                                           float listWidth,
                                           float indent) {
        rows.add(dataRow(label, value, rightReserveWidth, listWidth, indent));
    }

    private static void addDataRow(List<WimGuiListRow<StockReviewAction>> rows,
                                   String label,
                                   String value,
                                   float rightReserveWidth,
                                   float listWidth,
                                   float indent) {
        if (isMeaningful(value)) {
            rows.add(dataRow(label, value, rightReserveWidth, listWidth, indent));
        }
    }

    private static void addPositiveDataRow(List<WimGuiListRow<StockReviewAction>> rows,
                                           String label,
                                           String value,
                                           float rightReserveWidth,
                                           float listWidth,
                                           float indent) {
        if (isPositiveValue(value)) {
            rows.add(dataRow(label, value, rightReserveWidth, listWidth, indent));
        }
    }

    private static WimGuiListRow<StockReviewAction> dataRow(String label,
                                                            String value,
                                                            float rightReserveWidth,
                                                            float listWidth,
                                                            float indent) {
        return StockReviewListRow.labelTextIndented(
                label,
                value,
                indent,
                false,
                rightReserveWidth + StockReviewStyle.TEXT_LEFT_PAD,
                listWidth);
    }

    private static boolean isMeaningful(String value) {
        return value != null && value.trim().length() > 0 && !"?".equals(value.trim());
    }

    private static boolean isPositiveValue(String value) {
        if (!isMeaningful(value)) {
            return false;
        }
        try {
            return Float.parseFloat(value.replace("\u00b0/s", "").trim()) > 0f;
        } catch (NumberFormatException ex) {
            return true;
        }
    }
}
