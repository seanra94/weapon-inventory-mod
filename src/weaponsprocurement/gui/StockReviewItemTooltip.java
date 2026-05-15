package weaponsprocurement.gui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.combat.DamageType;
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
    private static final float CONTENT_WIDTH = 400f;
    private static final float OUTER_PAD_X = 12f;
    private static final float OUTER_PAD_TOP = 6f;
    private static final float OUTER_PAD_BOTTOM = 8f;
    private static final float WIDTH = CONTENT_WIDTH + 2f * OUTER_PAD_X;
    private static final float MAX_TOOLTIP_HEIGHT = 900f;
    private static final float SECTION_PAD = 10f;
    private static final float SMALL_PAD = 3f;
    private static final float SECTION_CONTENT_PAD = 14f;
    private static final float GRID_ROW_HEIGHT = 21f;
    private static final float ICON_SIZE = 80f;
    private static final float ICON_LEFT = 20f;
    private static final float ICON_TOP = 10f;
    private static final float ICON_INSET = 10f;
    private static final float ICON_GRID_GAP = 34f;
    private static final float GRID_WIDTH = CONTENT_WIDTH - ICON_LEFT - ICON_SIZE - ICON_GRID_GAP;
    private static final Color VANILLA_SECTION = new Color(9, 78, 88, 225);
    private static final Color ICON_BACKING = new Color(0, 0, 0, 220);
    private static final Color ICON_FRAME = new Color(230, 214, 0, 255);

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
        return WIDTH;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
        if (record.isWing()) {
            tooltip.setParaFontDefault();
            tooltip.setParaFontColor(textColor());
            tooltip.addTitle(record.getDisplayName(), titleColor());
            createWingTooltip(tooltip);
            if (WimGuiTooltip.hasText(toggleText)) {
                tooltip.addPara(toggleText, SECTION_PAD, StockReviewStyle.MUTED,
                        highlightColor(), "Basic Info", "Advanced Info");
            }
        } else {
            setCodexEntry(tooltip, CodexDataV2.getWeaponEntryId(record.getItemId()));
            addPaddedWeaponTooltip(tooltip);
        }
    }

    private void addPaddedWeaponTooltip(TooltipMakerAPI tooltip) {
        CustomPanelAPI panel = Global.getSettings().createCustom(WIDTH, MAX_TOOLTIP_HEIGHT, new BaseCustomUIPanelPlugin());
        TooltipMakerAPI content = panel.createUIElement(CONTENT_WIDTH, MAX_TOOLTIP_HEIGHT, false);
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
        addIconGrid(tooltip, weaponSpriteName(spec), primaryRows(spec), true, SECTION_CONTENT_PAD);
        addSpecPara(tooltip, spec.getCustomPrimary(), spec.getCustomPrimaryHL(), SMALL_PAD);

        addSectionHeading(tooltip, "Ancillary data", SECTION_PAD);
        addIconGrid(tooltip, damageIconSpriteName(spec.getDamageType()), ancillaryRows(spec), false, SECTION_CONTENT_PAD);
        addSpecPara(tooltip, spec.getCustomAncillary(), spec.getCustomAncillaryHL(), SMALL_PAD);
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
            LabelAPI label = tooltip.addPara(firstPara.trim(), SECTION_PAD);
            if (hasText(description.getText2()) && description.getText2().trim().startsWith("-")) {
                label.italicize();
            }
        }
        if (hasText(description.getText2()) && description.getText2().trim().startsWith("-")) {
            LabelAPI label = tooltip.addPara(description.getText2().trim(), SMALL_PAD, mutedColor());
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
        addSpacer(rows);
        addRow(rows, "Range", record.getRangeLabel());
        addRow(rows, damageLabel(spec), damageValue(spec));
        if (hasMeaningful(record.getEmpLabel()) && !"0".equals(record.getEmpLabel())) {
            addRow(rows, "EMP damage", record.getEmpLabel());
        }
        if (!spec.isNoDPSInTooltip()) {
            addRow(rows, "Damage / second", record.getSustainedDamagePerSecondLabel());
        }
        addSpacer(rows);
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
        addSpacer(rows);
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

    private void addIconGrid(TooltipMakerAPI tooltip, String spriteName, List<StatRow> rows, boolean framed, float pad) {
        if (rows.isEmpty()) {
            return;
        }
        int visibleRows = Math.max(1, rows.size());
        float height = Math.max(ICON_SIZE + ICON_TOP, visibleRows * GRID_ROW_HEIGHT);
        CustomPanelAPI panel = Global.getSettings().createCustom(CONTENT_WIDTH, height, new BaseCustomUIPanelPlugin());
        CustomPanelAPI icon = panel.createCustomPanel(ICON_SIZE, ICON_SIZE, new IconPanelPlugin(spriteName, framed));
        panel.addComponent(icon).inTL(ICON_LEFT, Math.min(ICON_TOP, Math.max(0f, height - ICON_SIZE)));

        TooltipMakerAPI grid = panel.createUIElement(GRID_WIDTH, height, false);
        grid.setParaFontDefault();
        grid.setParaFontColor(textColor());
        grid.setGridRowHeight(GRID_ROW_HEIGHT);
        grid.beginGrid(GRID_WIDTH, 1);
        grid.setGridLabelColor(textColor());
        grid.setGridValueColor(highlightColor());
        for (int i = 0; i < rows.size(); i++) {
            StatRow row = rows.get(i);
            grid.addToGrid(0, i, row.label, row.value);
        }
        grid.addGrid(0f);
        grid.resetGridRowHeight();
        panel.addUIElement(grid).inTL(ICON_LEFT + ICON_SIZE + ICON_GRID_GAP, 0f);
        tooltip.addCustom(panel, pad);
    }

    private static void addSectionHeading(TooltipMakerAPI tooltip, String text, float pad) {
        tooltip.addSectionHeading(text, VANILLA_SECTION, VANILLA_SECTION, Alignment.MID, pad);
    }

    private static void beginStyledGrid(TooltipMakerAPI tooltip) {
        tooltip.beginGrid(WIDTH, 3);
        tooltip.setGridLabelColor(textColor());
        tooltip.setGridValueColor(textColor());
    }

    private void addSpecPara(TooltipMakerAPI tooltip, String text, String highlight, float pad) {
        if (!hasText(text)) {
            return;
        }
        String[] highlights = splitHighlights(highlight);
        if (highlights.length > 0) {
            LabelAPI label = tooltip.addPara(text, pad, textColor(), highlightColor(), highlights);
            label.setHighlight(highlights);
            label.setHighlightColor(highlightColor());
            return;
        }
        tooltip.addPara(text, pad, textColor());
    }

    private void addHighlightedPara(TooltipMakerAPI tooltip, String text, String highlight, float pad) {
        LabelAPI label = tooltip.addPara(text, pad, textColor(), highlightColor(), highlight);
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

    private static String weaponSpriteName(WeaponSpecAPI spec) {
        if (hasText(spec.getTurretSpriteName())) {
            return spec.getTurretSpriteName();
        }
        return spec.getHardpointSpriteName();
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
        return Misc.getTextColor();
    }

    private static Color mutedColor() {
        return Misc.getGrayColor();
    }

    private static Color highlightColor() {
        return Misc.getHighlightColor();
    }

    private static void setCodexEntry(TooltipMakerAPI tooltip, String entryId) {
        if (hasText(entryId)) {
            tooltip.setCodexEntryId(entryId);
        }
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

    private static final class IconPanelPlugin extends BaseCustomUIPanelPlugin {
        private final String spriteName;
        private final boolean framed;
        private PositionAPI position;

        private IconPanelPlugin(String spriteName, boolean framed) {
            this.spriteName = spriteName;
            this.framed = framed;
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
            Misc.renderQuadAlpha(x, y, width, height, ICON_BACKING, alphaMult);
            if (framed) {
                renderFrame(x, y, width, height, alphaMult);
            }
            renderSprite(x, y, width, height, alphaMult);
        }

        private void renderFrame(float x, float y, float width, float height, float alphaMult) {
            float thickness = 4f;
            Misc.renderQuadAlpha(x, y, width, thickness, ICON_FRAME, alphaMult);
            Misc.renderQuadAlpha(x, y + height - thickness, width, thickness, ICON_FRAME, alphaMult);
            Misc.renderQuadAlpha(x, y, thickness, height, ICON_FRAME, alphaMult);
            Misc.renderQuadAlpha(x + width - thickness, y, thickness, height, ICON_FRAME, alphaMult);
        }

        private void renderSprite(float x, float y, float width, float height, float alphaMult) {
            if (!hasText(spriteName)) {
                return;
            }
            SpriteAPI sprite;
            try {
                sprite = Global.getSettings().getSprite(spriteName);
            } catch (RuntimeException ex) {
                return;
            }
            if (sprite == null || sprite.getWidth() <= 0f || sprite.getHeight() <= 0f) {
                return;
            }
            float oldWidth = sprite.getWidth();
            float oldHeight = sprite.getHeight();
            float oldAlpha = sprite.getAlphaMult();
            Color oldColor = sprite.getColor();
            float maxWidth = Math.max(1f, width - 2f * ICON_INSET);
            float maxHeight = Math.max(1f, height - 2f * ICON_INSET);
            float scale = Math.min(maxWidth / oldWidth, maxHeight / oldHeight);
            sprite.setSize(oldWidth * scale, oldHeight * scale);
            sprite.setAlphaMult(oldAlpha * alphaMult);
            sprite.setColor(Color.WHITE);
            sprite.renderAtCenter(x + width * 0.5f, y + height * 0.5f);
            sprite.setSize(oldWidth, oldHeight);
            sprite.setAlphaMult(oldAlpha);
            sprite.setColor(oldColor);
        }
    }
}
