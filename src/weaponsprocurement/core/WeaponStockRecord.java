package weaponsprocurement.core;

import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.MissileSpecAPI;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

import java.util.Collections;
import java.util.List;

public final class WeaponStockRecord {
    private final StockItemType itemType;
    private final String itemId;
    private final String itemKey;
    private final String displayName;
    private final WeaponSpecAPI spec;
    private final FighterWingSpecAPI wingSpec;
    private final int ownedCount;
    private final int playerCargoCount;
    private final int purchasableCount;
    private final int desiredCount;
    private final StockCategory category;
    private final List<SubmarketWeaponStock> submarketStocks;

    public WeaponStockRecord(String weaponId,
                             String displayName,
                             WeaponSpecAPI spec,
                             int ownedCount,
                             int playerCargoCount,
                             int purchasableCount,
                             int desiredCount,
                             StockCategory category,
                             List<SubmarketWeaponStock> submarketStocks) {
        this(StockItemType.WEAPON,
                weaponId,
                displayName,
                spec,
                null,
                ownedCount,
                playerCargoCount,
                purchasableCount,
                desiredCount,
                category,
                submarketStocks);
    }

    public WeaponStockRecord(StockItemType itemType,
                             String itemId,
                             String displayName,
                             WeaponSpecAPI spec,
                             FighterWingSpecAPI wingSpec,
                             int ownedCount,
                             int playerCargoCount,
                             int purchasableCount,
                             int desiredCount,
                             StockCategory category,
                             List<SubmarketWeaponStock> submarketStocks) {
        this.itemType = itemType == null ? StockItemType.WEAPON : itemType;
        this.itemId = itemId;
        this.itemKey = this.itemType.key(itemId);
        this.displayName = displayName;
        this.spec = spec;
        this.wingSpec = wingSpec;
        this.ownedCount = ownedCount;
        this.playerCargoCount = playerCargoCount;
        this.purchasableCount = purchasableCount;
        this.desiredCount = desiredCount;
        this.category = category;
        this.submarketStocks = Collections.unmodifiableList(submarketStocks);
    }

    public String getItemKey() {
        return itemKey;
    }

    public String getItemId() {
        return itemId;
    }

    public StockItemType getItemType() {
        return itemType;
    }

    public boolean isWing() {
        return StockItemType.WING.equals(itemType);
    }

    public String getDisplayName() {
        return displayName;
    }

    public WeaponSpecAPI getSpec() {
        return spec;
    }

    public FighterWingSpecAPI getWingSpec() {
        return wingSpec;
    }

    public int getOwnedCount() {
        return ownedCount;
    }

    public int getStorageCount() {
        return ownedCount;
    }

    public int getPlayerCargoCount() {
        return playerCargoCount;
    }

    public int getPurchasableCount() {
        return purchasableCount;
    }

    public int getBuyableCount() {
        int count = 0;
        for (SubmarketWeaponStock stock : submarketStocks) {
            if (stock.isPurchasable()) {
                count += stock.getCount();
            }
        }
        return count;
    }

    public int getNeededCount() {
        return Math.max(0, desiredCount - ownedCount);
    }

    public int getCheapestPurchasableUnitPrice() {
        int cheapest = Integer.MAX_VALUE;
        for (int i = 0; i < submarketStocks.size(); i++) {
            SubmarketWeaponStock stock = submarketStocks.get(i);
            if (!stock.isPurchasable() || stock.getCount() <= 0) {
                continue;
            }
            cheapest = Math.min(cheapest, stock.getUnitPrice());
        }
        return cheapest == Integer.MAX_VALUE ? Integer.MAX_VALUE : cheapest;
    }

    public int getDesiredCount() {
        return desiredCount;
    }

    public StockCategory getCategory() {
        return category;
    }

    public List<SubmarketWeaponStock> getSubmarketStocks() {
        return submarketStocks;
    }

    public String getSizeLabel() {
        if (isWing()) {
            return "Wing";
        }
        return valueOrUnknown(spec == null ? null : spec.getSize());
    }

    public String getTypeLabel() {
        if (isWing()) {
            return valueOrUnknown(wingSpec == null ? null : wingSpec.getRole());
        }
        return valueOrUnknown(spec == null ? null : spec.getType());
    }

    public String getPrimaryRoleLabel() {
        return valueOrUnknown(spec == null ? null : spec.getPrimaryRoleStr());
    }

    public String getOpCostLabel() {
        return spec == null ? "?" : String.valueOf(Math.round(spec.getOrdnancePointCost(null)));
    }

    public String getDamageTypeLabel() {
        return valueOrUnknown(spec == null ? null : spec.getDamageType());
    }

    public String getDamageLabel() {
        if (isWing()) {
            return "?";
        }
        return spec == null || spec.getDerivedStats() == null ? "?" : String.valueOf(Math.round(spec.getDerivedStats().getDamagePerShot()));
    }

    public String getEmpLabel() {
        if (isWing()) {
            return "?";
        }
        return spec == null || spec.getDerivedStats() == null ? "?" : String.valueOf(Math.round(spec.getDerivedStats().getEmpPerShot()));
    }

    public String getRangeLabel() {
        if (isWing()) {
            return wingSpec == null ? "?" : String.valueOf(Math.round(wingSpec.getRange()));
        }
        return spec == null ? "?" : String.valueOf(Math.round(spec.getMaxRange()));
    }

    public String getFluxPerSecondLabel() {
        if (isWing()) {
            return "?";
        }
        return spec == null || spec.getDerivedStats() == null ? "?" : String.valueOf(Math.round(spec.getDerivedStats().getFluxPerSecond()));
    }

    public String getFluxPerDamageLabel() {
        if (isWing()) {
            return "?";
        }
        return spec == null || spec.getDerivedStats() == null ? "?" : formatOneDecimal(spec.getDerivedStats().getFluxPerDam());
    }

    public String getRefireSecondsLabel() {
        ProjectileWeaponSpecAPI projectile = projectileWeaponSpec();
        return projectile == null ? "?" : formatTwoDecimals(projectile.getRefireDelay());
    }

    public String getSustainedDamagePerSecondLabel() {
        if (spec == null || spec.getDerivedStats() == null) {
            return "?";
        }
        int sustained = Math.round(spec.getDerivedStats().getSustainedDps());
        int burst = Math.round(spec.getDerivedStats().getDps());
        return sustained == burst ? String.valueOf(sustained) : sustained + " (" + burst + ")";
    }

    public boolean hasDifferentSustainedDamagePerSecond() {
        return spec != null
                && spec.getDerivedStats() != null
                && Math.round(spec.getDerivedStats().getSustainedDps()) != Math.round(spec.getDerivedStats().getDps());
    }

    public String getSustainedFluxPerSecondLabel() {
        if (spec == null || spec.getDerivedStats() == null) {
            return "?";
        }
        int sustained = Math.round(spec.getDerivedStats().getSustainedFluxPerSecond());
        int burst = Math.round(spec.getDerivedStats().getFluxPerSecond());
        return sustained == burst ? String.valueOf(sustained) : sustained + " (" + burst + ")";
    }

    public boolean hasDifferentSustainedFluxPerSecond() {
        return spec != null
                && spec.getDerivedStats() != null
                && Math.round(spec.getDerivedStats().getSustainedFluxPerSecond()) != Math.round(spec.getDerivedStats().getFluxPerSecond());
    }

    public String getSustainedEmpPerSecondLabel() {
        return spec == null || spec.getDerivedStats() == null ? "?" : String.valueOf(Math.round(spec.getDerivedStats().getEmpPerSecond()));
    }

    public boolean hasDifferentSustainedEmpPerSecond() {
        return false;
    }

    public String getFluxPerEmpLabel() {
        if (spec == null || spec.getDerivedStats() == null || spec.getDerivedStats().getEmpPerSecond() <= 0f) {
            return "?";
        }
        return String.valueOf(Math.round(spec.getDerivedStats().getSustainedFluxPerSecond() / spec.getDerivedStats().getEmpPerSecond()));
    }

    public String getBeamDpsLabel() {
        return spec == null || !spec.isBeam() || spec.getDerivedStats() == null ? "?" : String.valueOf(Math.round(spec.getDerivedStats().getDps()));
    }

    public String getBeamChargeUpLabel() {
        return spec == null || spec.getBeamChargeupTime() <= 0f ? "?" : formatTwoDecimals(spec.getBeamChargeupTime());
    }

    public String getBeamChargeDownLabel() {
        return spec == null || spec.getBeamChargedownTime() <= 0f ? "?" : formatTwoDecimals(spec.getBeamChargedownTime());
    }

    public String getBurstDelayLabel() {
        ProjectileWeaponSpecAPI projectile = projectileWeaponSpec();
        return projectile == null || projectile.getBurstDelay() <= 0f ? "?" : formatTwoDecimals(projectile.getBurstDelay());
    }

    public String getTurnRateLabel() {
        if (spec == null || spec.getTurnRate() <= 0f) {
            return "?";
        }
        return Math.round(spec.getTurnRate()) + "\u00b0/s";
    }

    public String getMinSpreadLabel() {
        return spec == null || spec.getMinSpread() <= 0f ? "?" : formatOneDecimal(spec.getMinSpread());
    }

    public String getMaxSpreadLabel() {
        return spec == null || spec.getMaxSpread() <= 0f ? "?" : formatOneDecimal(spec.getMaxSpread());
    }

    public String getSpreadPerShotLabel() {
        return spec == null || spec.getSpreadBuildup() <= 0f ? "?" : formatOneDecimal(spec.getSpreadBuildup());
    }

    public String getSpreadDecayLabel() {
        return spec == null || spec.getSpreadDecayRate() <= 0f ? "?" : formatOneDecimal(spec.getSpreadDecayRate());
    }

    public String getProjectileSpeedLabel() {
        ProjectileWeaponSpecAPI projectile = projectileWeaponSpec();
        if (projectile == null) {
            return "?";
        }
        float speed;
        try {
            speed = projectile.getProjectileSpeed(null, null);
        } catch (RuntimeException ex) {
            return "?";
        }
        return speed <= 0f || Float.isNaN(speed) || Float.isInfinite(speed)
                ? "?"
                : String.valueOf(Math.round(speed));
    }

    public String getLaunchSpeedLabel() {
        MissileSpecAPI missile = missileSpec();
        return missile == null || missile.getLaunchSpeed() <= 0f ? "?" : String.valueOf(Math.round(missile.getLaunchSpeed()));
    }

    public String getFlightTimeLabel() {
        MissileSpecAPI missile = missileSpec();
        return missile == null || missile.getMaxFlightTime() <= 0f ? "?" : formatTwoDecimals(missile.getMaxFlightTime());
    }

    public String getGuidedLabel() {
        MissileSpecAPI missile = missileSpec();
        if (missile == null) {
            return "?";
        }
        String tracking = spec == null ? null : spec.getTrackingStr();
        boolean guided = tracking != null && tracking.trim().length() > 0 && !"None".equalsIgnoreCase(tracking.trim());
        return guided ? "TRUE" : "FALSE";
    }

    public String getMaxAmmoLabel() {
        return spec == null || !spec.usesAmmo() || spec.getMaxAmmo() <= 0 ? "?" : String.valueOf(spec.getMaxAmmo());
    }

    public String getSecPerReloadLabel() {
        if (spec == null || !spec.usesAmmo() || spec.getAmmoPerSecond() <= 0f || spec.getReloadSize() <= 0f) {
            return "?";
        }
        return formatTwoDecimals(spec.getReloadSize() / spec.getAmmoPerSecond());
    }

    public String getAmmoGainLabel() {
        return spec == null || !spec.usesAmmo() || spec.getAmmoPerSecond() <= 0f ? "?" : formatOneDecimal(spec.getAmmoPerSecond());
    }

    public String getAccuracyLabel() {
        return valueOrUnknown(spec == null ? null : spec.getAccuracyStr());
    }

    public String getWingFighterCountLabel() {
        return wingSpec == null ? "?" : String.valueOf(wingSpec.getNumFighters());
    }

    public String getWingOpCostLabel() {
        return wingSpec == null ? "?" : String.valueOf(Math.round(wingSpec.getOpCost(null)));
    }

    public String getWingRefitTimeLabel() {
        return wingSpec == null ? "?" : formatOneDecimal(wingSpec.getRefitTime()) + "s";
    }

    private static String formatOneDecimal(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return "?";
        }
        return String.valueOf(Math.round(value * 10f) / 10f);
    }

    private static String formatTwoDecimals(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return "?";
        }
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private ProjectileWeaponSpecAPI projectileWeaponSpec() {
        return spec instanceof ProjectileWeaponSpecAPI ? (ProjectileWeaponSpecAPI) spec : null;
    }

    private MissileSpecAPI missileSpec() {
        Object projectile = spec == null ? null : spec.getProjectileSpec();
        return projectile instanceof MissileSpecAPI ? (MissileSpecAPI) projectile : null;
    }

    private static String valueOrUnknown(Object value) {
        return value == null ? "?" : String.valueOf(value);
    }
}
