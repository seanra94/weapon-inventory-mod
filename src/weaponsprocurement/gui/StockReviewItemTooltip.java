package weaponsprocurement.gui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import weaponsprocurement.core.CreditFormat;
import weaponsprocurement.core.SubmarketWeaponStock;
import weaponsprocurement.core.WeaponStockRecord;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class StockReviewItemTooltip implements TooltipMakerAPI.TooltipCreator {
    private static final float VANILLA_TOOLTIP_WIDTH = 400f;
    private static final float CONTENT_WIDTH = VANILLA_TOOLTIP_WIDTH * 1.25f;
    private static final float WING_TOOLTIP_WIDTH = 424f;
    private static final float OUTER_PAD_X = 16f;
    private static final float OUTER_PAD_TOP = 8f;
    private static final float OUTER_PAD_BOTTOM = OUTER_PAD_X;
    private static final float WIDTH = CONTENT_WIDTH + 2f * OUTER_PAD_X;
    private static final float TOOLTIP_LAYOUT_HEIGHT = 1400f;
    private static final float SECTION_PAD = 9f;
    private static final float SMALL_PAD = 4f;
    private static final float SECTION_CONTENT_PAD = 12f;
    private static final float CUSTOM_TEXT_PAD = 6f;
    private static final float GRID_BOTTOM_PAD = 8f;
    private static final float GRID_ROW_HEIGHT = 24f;
    private static final float SECTION_HEADING_HEIGHT = 22f;
    private static final float ICON_SIZE = 92f;
    private static final float ICON_LEFT = 28f;
    private static final float ICON_TOP = 12f;
    private static final float ICON_INSET = 2f;
    private static final float ICON_GRID_GAP = 44f;
    private static final float GRID_WIDTH = CONTENT_WIDTH - ICON_LEFT - ICON_SIZE - ICON_GRID_GAP - 8f;
    private static final float GRID_LABEL_WIDTH = 188f;
    private static final Color VANILLA_SECTION = new Color(9, 78, 88, 225);
    private static final Color TOOLTIP_TEXT = new Color(215, 215, 215, 255);
    private static final Color TOOLTIP_MUTED = new Color(175, 175, 175, 255);
    private static final int DESCRIPTION_MAX_LINES = 4;
    private static final int CUSTOM_TEXT_MAX_LINES = 3;
    private static final float ESTIMATED_DESCRIPTION_CHAR_WIDTH = 8f;

    private final WeaponStockRecord record;
    private final String toggleText;

    private StockReviewItemTooltip(WeaponStockRecord record, String toggleText) {
        this.record = record;
        this.toggleText = toggleText;
    }

    static TooltipMakerAPI.TooltipCreator forRecord(WeaponStockRecord record, String toggleText) {
        if (record == null) {
            return null;
        }
        if (record.isWing() && record.getWingSpec() == null) {
            return null;
        }
        if (!record.isWing() && record.getSpec() == null) {
            return null;
        }
        return new StockReviewItemTooltip(record, toggleText);
    }

    @Override
    public boolean isTooltipExpandable(Object tooltipParam) {
        return false;
    }

    @Override
    public float getTooltipWidth(Object tooltipParam) {
        return record.isWing() ? WING_TOOLTIP_WIDTH : WIDTH;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        if (record.isWing()) {
            tooltip.setParaFontDefault();
            tooltip.setParaFontColor(textColor());
            tooltip.addTitle(record.getDisplayName(), titleColor());
            createWingTooltip(tooltip);
            if (WimGuiTooltip.hasText(toggleText)) {
                tooltip.addPara(tooltipFormat(toggleText), SECTION_PAD, StockReviewStyle.MUTED,
                        highlightColor(), "Basic Info", "Advanced Info");
            }
        } else {
            setCodexEntry(tooltip, CodexDataV2.getWeaponEntryId(record.getItemId()));
            addPaddedWeaponTooltip(tooltip);
        }
    }

    private void addPaddedWeaponTooltip(TooltipMakerAPI tooltip) {
        CustomPanelAPI panel = Global.getSettings().createCustom(WIDTH, TOOLTIP_LAYOUT_HEIGHT, new BaseCustomUIPanelPlugin());
        TooltipMakerAPI content = panel.createUIElement(CONTENT_WIDTH, TOOLTIP_LAYOUT_HEIGHT, false);
        content.setParaFontDefault();
        content.setParaFontColor(textColor());
        createWeaponTooltip(content);

        float contentHeight = Math.max(1f, content.getHeightSoFar());
        content.getPosition().setSize(CONTENT_WIDTH, contentHeight);
        panel.addUIElement(content).inTL(OUTER_PAD_X, OUTER_PAD_TOP);
        panel.getPosition().setSize(WIDTH, contentHeight + OUTER_PAD_TOP + OUTER_PAD_BOTTOM);
        tooltip.addCustom(panel, 0f);
    }

    private void createWeaponTooltip(TooltipMakerAPI tooltip) {
        WeaponSpecAPI spec = record.getSpec();
        tooltip.addTitle(record.getDisplayName(), titleColor());
        Misc.addDesignTypePara(tooltip, spec.getManufacturer(), SMALL_PAD);
        addDescription(tooltip);
        addCargoContext(tooltip);

        addSectionHeading(tooltip, "Primary data", SECTION_PAD);
        addIconGrid(tooltip, StockReviewWeaponIconPlugin.spriteName(spec), primaryRows(spec), true,
                StockReviewWeaponIconPlugin.motifType(spec), SECTION_CONTENT_PAD);
        addSpecPara(tooltip, spec.getCustomPrimary(), spec.getCustomPrimaryHL(), CUSTOM_TEXT_PAD, spec);

        addSectionHeading(tooltip, "Ancillary data", SECTION_PAD);
        addIconGrid(tooltip, damageIconSpriteName(spec.getDamageType()), ancillaryRows(spec), false,
                null, SECTION_CONTENT_PAD);
        addSpecPara(tooltip, spec.getCustomAncillary(), spec.getCustomAncillaryHL(), CUSTOM_TEXT_PAD, spec);
    }

    private void createWingTooltip(TooltipMakerAPI tooltip) {
        FighterWingSpecAPI spec = record.getWingSpec();
        setCodexEntry(tooltip, CodexDataV2.getFighterEntryId(record.getItemId()));
        tooltip.addSectionHeading("Fighter LPC", StockReviewStyle.HEADING_BACKGROUND,
                StockReviewStyle.ROW_BORDER, Alignment.MID, SECTION_PAD);
        beginStyledGrid(tooltip);
        tooltip.addToGrid(0, 0, "Role", format(spec.getRole()));
        tooltip.addToGrid(1, 0, "OP", record.getWingOpCostLabel());
        tooltip.addToGrid(2, 0, "Fighters", record.getWingFighterCountLabel());
        tooltip.addToGrid(0, 1, "Range", record.getRangeLabel());
        tooltip.addToGrid(1, 1, "Refit", record.getWingRefitTimeLabel());
        tooltip.addToGrid(2, 1, "Desired", String.valueOf(record.getDesiredCount()));
        tooltip.addGrid(SMALL_PAD);
    }

    private void addDescription(TooltipMakerAPI tooltip) {
        Description description = null;
        try {
            description = Global.getSettings().getDescription(record.getItemId(), Description.Type.WEAPON);
        } catch (RuntimeException ignored) {
        }
        if (description == null) {
            return;
        }
        String firstPara = description.getText1FirstPara();
        if (hasText(firstPara)) {
            LabelAPI label = tooltip.addPara(tooltipFormat(truncateDescription(firstPara.trim())), SECTION_PAD);
            if (hasText(description.getText2()) && description.getText2().trim().startsWith("-")) {
                label.italicize();
            }
        }
        if (hasText(description.getText2()) && description.getText2().trim().startsWith("-")) {
            LabelAPI label = tooltip.addPara(tooltipFormat(description.getText2().trim()), SMALL_PAD, mutedColor());
            label.italicize();
        }
    }

    private void addCargoContext(TooltipMakerAPI tooltip) {
        String cargoSpace = cargoSpaceLabel();
        if (hasText(cargoSpace)) {
            addHighlightedPara(tooltip, "Cargo space: " + cargoSpace + " per unit.", cargoSpace, SECTION_PAD);
        }

        String price = priceLabel();
        if (hasText(price)) {
            addHighlightedPara(tooltip, "Price: " + price + " per unit.", price, SECTION_PAD);
        }

        String count = String.valueOf(record.getOwnedCount());
        String plural = record.getOwnedCount() == 1 ? "weapon" : "weapons";
        addHighlightedPara(tooltip, "You own a total of " + count + " " + plural + " of this type.", count, SECTION_PAD);
    }

    private List<StatRow> primaryRows(WeaponSpecAPI spec) {
        List<StatRow> rows = new ArrayList<StatRow>();
        addRow(rows, "Primary role", format(spec.getPrimaryRoleStr()));
        addRow(rows, "Mount type", format(spec.getSize()) + ", " + format(spec.getMountType()));
        addMountNotes(rows, spec);
        addRow(rows, "Ordnance points", record.getOpCostLabel());
        addRow(rows, "Range", record.getRangeLabel());
        addRow(rows, damageLabel(spec), damageValue(spec));
        if (hasMeaningful(record.getEmpLabel()) && !"0".equals(record.getEmpLabel())) {
            addRow(rows, "EMP damage", record.getEmpLabel());
        }
        if (!spec.isNoDPSInTooltip()) {
            addRow(rows, "Damage / second", record.getSustainedDamagePerSecondLabel());
        }
        addRow(rows, "Flux / second", record.getSustainedFluxPerSecondLabel());
        addRow(rows, "Flux / shot", fluxPerShotLabel(spec));
        addRow(rows, "Flux / damage", record.getFluxPerDamageLabel());
        return rows;
    }

    private List<StatRow> ancillaryRows(WeaponSpecAPI spec) {
        List<StatRow> rows = new ArrayList<StatRow>();
        DamageType damageType = spec.getDamageType();
        addRow(rows, "Damage type", damageType == null ? "?" : damageType.getDisplayName());
        addRow(rows, "", damageMultiplierLabel(damageType));
        addRow(rows, "Speed", format(spec.getSpeedStr()));
        addRow(rows, "Tracking", format(spec.getTrackingStr()));
        addRow(rows, "Accuracy", format(spec.getAccuracyStr()));
        addRow(rows, "Turn rate", format(spec.getTurnRateStr()));
        if (spec.getBurstSize() > 1) {
            addRow(rows, "Burst size", String.valueOf(spec.getBurstSize()));
        }
        addRow(rows, "Refire delay (seconds)", record.getRefireSecondsLabel());
        if (spec.usesAmmo()) {
            addRow(rows, "Ammo", record.getMaxAmmoLabel());
            addRow(rows, "Recharge / second", record.getAmmoGainLabel());
            addRow(rows, "Reload time (seconds)", record.getSecPerReloadLabel());
        }
        if (spec.isBeam()) {
            addRow(rows, "Charge up", record.getBeamChargeUpLabel());
            addRow(rows, "Charge down", record.getBeamChargeDownLabel());
        }
        return rows;
    }

    private void addIconGrid(TooltipMakerAPI tooltip,
                             String spriteName,
                             List<StatRow> rows,
                             boolean weaponTile,
                             WeaponAPI.WeaponType motifType,
                             float pad) {
        if (rows.isEmpty()) {
            return;
        }
        int visibleRows = Math.max(1, rows.size());
        float height = Math.max(ICON_SIZE + ICON_TOP, visibleRows * GRID_ROW_HEIGHT);
        CustomPanelAPI panel = Global.getSettings().createCustom(CONTENT_WIDTH, height, new BaseCustomUIPanelPlugin());
        CustomPanelAPI icon = panel.createCustomPanel(ICON_SIZE, ICON_SIZE,
                weaponTile
                        ? new StockReviewWeaponIconPlugin(spriteName, motifType)
                        : new IconPanelPlugin(spriteName));
        panel.addComponent(icon).inTL(ICON_LEFT, Math.min(ICON_TOP, Math.max(0f, height - ICON_SIZE)));

        for (int i = 0; i < rows.size(); i++) {
            StatRow row = rows.get(i);
            addStatRow(panel, ICON_LEFT + ICON_SIZE + ICON_GRID_GAP, i * GRID_ROW_HEIGHT,
                    GRID_WIDTH, GRID_ROW_HEIGHT, row);
        }
        tooltip.addCustom(panel, pad);
        tooltip.addSpacer(GRID_BOTTOM_PAD);
    }

    private static void addStatRow(CustomPanelAPI panel,
                                   float x,
                                   float y,
                                   float width,
                                   float height,
                                   StatRow row) {
        if (row == null) {
            return;
        }
        if (!hasText(row.label)) {
            addPanelLabel(panel, row.value, highlightColor(), x, y, width, height, Alignment.RMID);
            return;
        }
        float valueX = x + GRID_LABEL_WIDTH;
        float valueWidth = Math.max(20f, width - GRID_LABEL_WIDTH);
        addPanelLabel(panel, row.label, textColor(), x, y, GRID_LABEL_WIDTH, height, Alignment.LMID);
        addPanelLabel(panel, row.value, highlightColor(), valueX, y, valueWidth, height, Alignment.RMID);
    }

    private static void addSectionHeading(TooltipMakerAPI tooltip, String text, float pad) {
        CustomPanelAPI panel = Global.getSettings().createCustom(CONTENT_WIDTH, SECTION_HEADING_HEIGHT,
                new SectionHeadingPlugin());
        addPanelLabel(panel, text, textColor(), 0f, 0f, CONTENT_WIDTH,
                SECTION_HEADING_HEIGHT, Alignment.MID);
        tooltip.addCustom(panel, pad);
    }

    private static void addPanelLabel(CustomPanelAPI parent,
                                      String text,
                                      Color color,
                                      float x,
                                      float y,
                                      float width,
                                      float height,
                                      Alignment alignment) {
        float labelX = x;
        float labelWidth = width;
        if (Alignment.LMID.equals(alignment)) {
            labelX += WimGuiStyle.TEXT_LEFT_PAD;
            labelWidth = Math.max(8f, width - WimGuiStyle.TEXT_LEFT_PAD);
        }
        TooltipMakerAPI label = parent.createUIElement(labelWidth, height, false);
        label.setParaFontDefault();
        label.setParaFontColor(color);
        int maxChars = WimGuiText.estimatedChars(labelWidth);
        LabelAPI line = label.addPara(tooltipFormat(WimGuiText.fit(text, maxChars)), 0f, color);
        line.setAlignment(alignment);
        parent.addUIElement(label).inTL(labelX, y + WimGuiStyle.TEXT_TOP_PAD);
    }

    private static void beginStyledGrid(TooltipMakerAPI tooltip) {
        tooltip.beginGrid(WING_TOOLTIP_WIDTH, 3);
        tooltip.setGridLabelColor(textColor());
        tooltip.setGridValueColor(textColor());
    }

    private void addSpecPara(TooltipMakerAPI tooltip,
                             String text,
                             String highlight,
                             float pad,
                             WeaponSpecAPI spec) {
        if (!hasText(text)) {
            return;
        }
        tooltip.addSpacer(SMALL_PAD);
        String[] rawHighlights = splitHighlights(highlight);
        String substitutedText = substituteFormatSpecifiers(text, rawHighlights, spec);
        String displayText = truncateForLines(substitutedText, CUSTOM_TEXT_MAX_LINES, CONTENT_WIDTH);
        String[] highlights = visibleHighlights(displayText, rawHighlights);
        if (highlights.length > 0) {
            LabelAPI label = tooltip.addPara(tooltipFormat(displayText), pad, textColor(), highlightColor(), highlights);
            label.setHighlight(highlights);
            label.setHighlightColor(highlightColor());
            tooltip.addSpacer(SMALL_PAD);
            return;
        }
        tooltip.addPara(tooltipFormat(displayText), pad, textColor());
        tooltip.addSpacer(SMALL_PAD);
    }

    private void addHighlightedPara(TooltipMakerAPI tooltip, String text, String highlight, float pad) {
        LabelAPI label = tooltip.addPara(tooltipFormat(text), pad, textColor(), highlightColor(), highlight);
        label.setHighlight(highlight);
        label.setHighlightColor(highlightColor());
    }

    private String cargoSpaceLabel() {
        float cargoSpace = unitCargoSpace();
        return validNumber(cargoSpace) ? formatOneDecimalTrim(cargoSpace) : null;
    }

    private float unitCargoSpace() {
        List<SubmarketWeaponStock> stocks = record.getSubmarketStocks();
        for (int i = 0; i < stocks.size(); i++) {
            float value = stocks.get(i).getUnitCargoSpace();
            if (validNumber(value) && value > 0f) {
                return value;
            }
        }
        try {
            CargoStackAPI stack = Global.getSettings().createCargoStack(CargoAPI.CargoItemType.WEAPONS, record.getItemId(), null);
            if (stack != null) {
                float value = stack.getCargoSpacePerUnit();
                if (validNumber(value) && value > 0f) {
                    return value;
                }
            }
        } catch (RuntimeException ignored) {
        }
        return Float.NaN;
    }

    private String priceLabel() {
        int price = record.getCheapestPurchasableUnitPrice();
        if (price == Integer.MAX_VALUE) {
            price = Math.round(Math.max(0f, record.getSpec().getBaseValue()));
        }
        return price <= 0 ? null : CreditFormat.credits(price);
    }

    private static String damageIconSpriteName(DamageType type) {
        String key = "icon_other";
        if (DamageType.KINETIC.equals(type)) {
            key = "icon_kinetic";
        } else if (DamageType.HIGH_EXPLOSIVE.equals(type)) {
            key = "icon_high_explosive";
        } else if (DamageType.FRAGMENTATION.equals(type)) {
            key = "icon_fragmentation";
        } else if (DamageType.ENERGY.equals(type)) {
            key = "icon_energy";
        }
        try {
            return Global.getSettings().getSpriteName("ui", key);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String damageLabel(WeaponSpecAPI spec) {
        return spec.hasTag("damage_special") ? "Special" : "Damage";
    }

    private String damageValue(WeaponSpecAPI spec) {
        if (spec.hasTag("damage_special")) {
            return "Special";
        }
        String damage = record.getDamageLabel();
        if (!hasMeaningful(damage)) {
            return damage;
        }
        int burstSize = spec.getBurstSize();
        if (!spec.isBeam() && burstSize > 1) {
            return damage + "x" + burstSize;
        }
        return damage;
    }

    private static void addMountNotes(List<StatRow> rows, WeaponSpecAPI spec) {
        String required = requiredMountSlots(spec);
        if (hasText(required)) {
            addRow(rows, "", required);
        }
        if (spec.getType() != null && spec.getMountType() != null && !spec.getType().equals(spec.getMountType())) {
            addRow(rows, "", "Counts as " + format(spec.getType()) + " for stat modifiers");
        }
    }

    private static String requiredMountSlots(WeaponSpecAPI spec) {
        if (spec.getMountType() == null || spec.getType() == null || spec.getMountType().equals(spec.getType())) {
            return null;
        }
        switch (spec.getMountType()) {
            case COMPOSITE:
                return "Requires a Ballistic, Missile, or Composite slot";
            case HYBRID:
                return "Requires a Ballistic, Energy, or Hybrid slot";
            case SYNERGY:
                return "Requires an Energy, Missile, or Synergy slot";
            case UNIVERSAL:
                return "Requires a Ballistic, Energy, Missile, or Universal slot";
            default:
                return null;
        }
    }

    private String fluxPerShotLabel(WeaponSpecAPI spec) {
        ProjectileWeaponSpecAPI projectile = projectileSpec(spec);
        if (projectile == null) {
            return "?";
        }
        float energy = projectile.getEnergyPerShot();
        return validNumber(energy) && energy > 0f ? String.valueOf(Math.round(energy)) : "0";
    }

    private static ProjectileWeaponSpecAPI projectileSpec(WeaponSpecAPI spec) {
        return spec instanceof ProjectileWeaponSpecAPI ? (ProjectileWeaponSpecAPI) spec : null;
    }

    private static String damageMultiplierLabel(DamageType damageType) {
        if (damageType == null) {
            return "?";
        }
        int shield = Math.round(damageType.getShieldMult() * 100f);
        int armor = Math.round(damageType.getArmorMult() * 100f);
        int hull = Math.round(damageType.getHullMult() * 100f);
        if (DamageType.KINETIC.equals(damageType)) {
            return shield + "% vs shields, " + armor + "% vs armor";
        }
        if (DamageType.HIGH_EXPLOSIVE.equals(damageType)) {
            return armor + "% vs armor, " + shield + "% vs shields";
        }
        if (DamageType.FRAGMENTATION.equals(damageType)) {
            if (shield == armor) {
                return shield + "% vs shields and armor, " + hull + "% vs hull";
            }
            return shield + "% vs shields, " + armor + "% vs armor, " + hull + "% vs hull";
        }
        if (shield == 100 && armor == 100 && hull == 100) {
            return damageType.getDescription();
        }
        List<String> parts = new ArrayList<String>();
        parts.add(shield + "% vs shields");
        parts.add(armor + "% vs armor");
        parts.add(hull + "% vs hull");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append(parts.get(i));
        }
        return result.toString();
    }

    private static void addRow(List<StatRow> rows, String label, String value) {
        if (!hasMeaningful(value)) {
            return;
        }
        rows.add(new StatRow(label, value));
    }

    private static void addSpacer(List<StatRow> rows) {
        if (!rows.isEmpty() && !rows.get(rows.size() - 1).isSpacer()) {
            rows.add(new StatRow("", ""));
        }
    }

    private static String[] splitHighlights(String highlight) {
        if (!hasText(highlight)) {
            return new String[0];
        }
        String[] raw = highlight.split("\\|");
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < raw.length; i++) {
            String trimmed = raw[i].trim();
            if (trimmed.length() > 0) {
                result.add(trimmed);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    private static Color titleColor() {
        return Misc.getTooltipTitleAndLightHighlightColor();
    }

    private static Color textColor() {
        return TOOLTIP_TEXT;
    }

    private static Color mutedColor() {
        return TOOLTIP_MUTED;
    }

    private static Color highlightColor() {
        return Misc.getHighlightColor();
    }

    private static void setCodexEntry(TooltipMakerAPI tooltip, String entryId) {
        if (hasText(entryId)) {
            tooltip.setCodexEntryId(entryId);
        }
    }

    private static String truncateDescription(String text) {
        if (!hasText(text)) {
            return text;
        }
        return truncateForLines(text, DESCRIPTION_MAX_LINES, CONTENT_WIDTH);
    }

    private static String truncateForLines(String text, int maxLines, float width) {
        if (!hasText(text)) {
            return text;
        }
        String normalized = text.trim().replaceAll("\\s+", " ");
        if (maxLines <= 0) {
            return normalized;
        }
        int charsPerLine = Math.max(32, (int) Math.floor(CONTENT_WIDTH / ESTIMATED_DESCRIPTION_CHAR_WIDTH));
        if (validNumber(width) && width > 0f) {
            charsPerLine = Math.max(32, (int) Math.floor(width / ESTIMATED_DESCRIPTION_CHAR_WIDTH));
        }
        String[] words = normalized.split(" ");
        StringBuilder result = new StringBuilder(normalized.length());
        int line = 1;
        int lineChars = 0;
        boolean truncated = false;
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.length() <= 0) {
                continue;
            }
            int addedChars = lineChars <= 0 ? word.length() : word.length() + 1;
            if (lineChars > 0 && lineChars + addedChars > charsPerLine) {
                line++;
                lineChars = 0;
                addedChars = word.length();
            }
            if (line > maxLines) {
                truncated = true;
                break;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(word);
            lineChars += addedChars;
        }
        if (!truncated && result.length() == normalized.length()) {
            return normalized;
        }
        return trimForEllipsis(result.toString()) + "...";
    }

    private static String[] visibleHighlights(String text, String[] highlights) {
        if (!hasText(text) || highlights == null || highlights.length <= 0) {
            return new String[0];
        }
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < highlights.length; i++) {
            String highlight = highlights[i];
            if (hasText(highlight) && text.indexOf(highlight) >= 0) {
                result.add(highlight);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    private static String substituteFormatSpecifiers(String text, String[] highlights, WeaponSpecAPI spec) {
        if (!hasText(text)) {
            return text;
        }
        StringBuilder result = new StringBuilder(text.length());
        int highlightIndex = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '%' || i + 1 >= text.length()) {
                result.append(c);
                continue;
            }
            char next = text.charAt(i + 1);
            if (next == '%') {
                result.append('%');
                i++;
                continue;
            }
            if (next == 's' || next == 'd' || next == 'f') {
                result.append(formatHighlightValue(highlights, highlightIndex, spec));
                highlightIndex++;
                i++;
                continue;
            }
            result.append(c);
        }
        return result.toString();
    }

    private static String formatHighlightValue(String[] highlights, int index, WeaponSpecAPI spec) {
        if (highlights != null && index >= 0 && index < highlights.length && hasText(highlights[index])) {
            return highlights[index].trim();
        }
        if (index == 0 && spec != null && spec.getDerivedStats() != null) {
            float value = spec.isBeam()
                    ? spec.getDerivedStats().getDps()
                    : spec.getDerivedStats().getDamagePerShot();
            if (validNumber(value) && value > 0f) {
                return formatOneDecimalTrim(value);
            }
        }
        return "?";
    }

    private static String trimForEllipsis(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith(",") || trimmed.endsWith(";") || trimmed.endsWith(":") || trimmed.endsWith(".")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private static String tooltipFormat(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("%", "%%");
    }

    private static boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }

    private static boolean hasMeaningful(String value) {
        if (!hasText(value)) {
            return false;
        }
        String trimmed = value.trim();
        return !"?".equals(trimmed) && !"---".equals(trimmed) && !"None".equalsIgnoreCase(trimmed);
    }

    private static boolean validNumber(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    private static String format(Object value) {
        if (value == null) {
            return "?";
        }
        String text = String.valueOf(value).replace('_', ' ').trim();
        if (text.length() <= 0 || "?".equals(text)) {
            return "?";
        }
        text = text.toLowerCase(Locale.US);
        StringBuilder result = new StringBuilder(text.length());
        boolean capitalize = true;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c) || c == '/' || c == '-') {
                capitalize = true;
                result.append(c);
            } else if (capitalize) {
                result.append(Character.toUpperCase(c));
                capitalize = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static String formatOneDecimalTrim(float value) {
        if (!validNumber(value)) {
            return "?";
        }
        int rounded = Math.round(value);
        if (Math.abs(value - rounded) < 0.05f) {
            return String.valueOf(rounded);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private static final class StatRow {
        private final String label;
        private final String value;

        private StatRow(String label, String value) {
            this.label = label == null ? "" : label;
            this.value = value == null ? "" : value;
        }

        private boolean isSpacer() {
            return label.length() <= 0 && value.length() <= 0;
        }
    }

    private static final class SectionHeadingPlugin extends BaseCustomUIPanelPlugin {
        private PositionAPI position;

        @Override
        public void positionChanged(PositionAPI position) {
            this.position = position;
        }

        @Override
        public void renderBelow(float alphaMult) {
            if (position == null) {
                return;
            }
            Misc.renderQuadAlpha(position.getX(), position.getY(), position.getWidth(),
                    position.getHeight(), VANILLA_SECTION, alphaMult);
        }
    }

    private static final class IconPanelPlugin extends BaseCustomUIPanelPlugin {
        private final String spriteName;
        private PositionAPI position;

        private IconPanelPlugin(String spriteName) {
            this.spriteName = spriteName;
        }

        @Override
        public void positionChanged(PositionAPI position) {
            this.position = position;
        }

        @Override
        public void render(float alphaMult) {
            if (position == null) {
                return;
            }
            float x = position.getX();
            float y = position.getY();
            float width = position.getWidth();
            float height = position.getHeight();
            renderSprite(x, y, width, height, ICON_INSET, alphaMult);
        }

        private void renderSprite(float x, float y, float width, float height, float inset, float alphaMult) {
            float maxWidth = Math.max(1f, width - 2f * inset);
            float maxHeight = Math.max(1f, height - 2f * inset);
            renderFittedSprite(spriteName, Color.WHITE, x + width * 0.5f, y + height * 0.5f,
                    maxWidth, maxHeight, alphaMult);
        }

        private boolean renderFittedSprite(String path,
                                           Color color,
                                           float centerX,
                                           float centerY,
                                           float maxWidth,
                                           float maxHeight,
                                           float alphaMult) {
            if (!hasText(path)) {
                return false;
            }
            SpriteAPI sprite;
            try {
                sprite = Global.getSettings().getSprite(path);
            } catch (RuntimeException ex) {
                return false;
            }
            if (sprite == null || sprite.getWidth() <= 0f || sprite.getHeight() <= 0f) {
                return false;
            }
            float oldWidth = sprite.getWidth();
            float oldHeight = sprite.getHeight();
            float oldAlpha = sprite.getAlphaMult();
            Color oldColor = sprite.getColor();
            float oldAngle = sprite.getAngle();
            float scale = Math.min(Math.max(1f, maxWidth) / oldWidth, Math.max(1f, maxHeight) / oldHeight);
            sprite.setSize(oldWidth * scale, oldHeight * scale);
            sprite.setAlphaMult(oldAlpha * alphaMult);
            sprite.setColor(color == null ? Color.WHITE : color);
            sprite.setAngle(0f);
            sprite.renderAtCenter(centerX, centerY);
            sprite.setSize(oldWidth, oldHeight);
            sprite.setAlphaMult(oldAlpha);
            sprite.setColor(oldColor);
            sprite.setAngle(oldAngle);
            return true;
        }
    }
}
