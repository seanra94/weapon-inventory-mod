package weaponinventorymod.core;

import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

import java.util.Collections;
import java.util.List;

public final class WeaponStockRecord {
    private final String weaponId;
    private final String displayName;
    private final WeaponSpecAPI spec;
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
        this.weaponId = weaponId;
        this.displayName = displayName;
        this.spec = spec;
        this.ownedCount = ownedCount;
        this.playerCargoCount = playerCargoCount;
        this.purchasableCount = purchasableCount;
        this.desiredCount = desiredCount;
        this.category = category;
        this.submarketStocks = Collections.unmodifiableList(submarketStocks);
    }

    public String getWeaponId() {
        return weaponId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public WeaponSpecAPI getSpec() {
        return spec;
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

    public int getStoredOutsideInventoryCount() {
        return Math.max(0, ownedCount - playerCargoCount);
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

    public String getCountLabel() {
        return ownedCount + "/" + purchasableCount;
    }

    public String getDetailLine() {
        WeaponAPI.WeaponSize size = spec == null ? null : spec.getSize();
        String role = spec == null ? null : spec.getPrimaryRoleStr();
        DamageType damageType = spec == null ? null : spec.getDamageType();
        int op = spec == null ? 0 : Math.round(spec.getOrdnancePointCost(null));
        int range = spec == null ? 0 : Math.round(spec.getMaxRange());
        return "Desired: " + desiredCount
                + " | Size: " + valueOrUnknown(size)
                + " | Role: " + valueOrUnknown(role)
                + " | OP: " + op
                + " | Range: " + range
                + " | Damage: " + valueOrUnknown(damageType);
    }

    public String getSizeLabel() {
        return valueOrUnknown(spec == null ? null : spec.getSize());
    }

    public String getTypeLabel() {
        return valueOrUnknown(spec == null ? null : spec.getType());
    }

    public String getDamageTypeLabel() {
        return valueOrUnknown(spec == null ? null : spec.getDamageType());
    }

    public String getDamageLabel() {
        return spec == null || spec.getDerivedStats() == null ? "?" : String.valueOf(Math.round(spec.getDerivedStats().getDamagePerShot()));
    }

    public String getEmpLabel() {
        return spec == null || spec.getDerivedStats() == null ? "?" : String.valueOf(Math.round(spec.getDerivedStats().getEmpPerShot()));
    }

    public String getRangeLabel() {
        return spec == null ? "?" : String.valueOf(Math.round(spec.getMaxRange()));
    }

    public String getFluxPerSecondLabel() {
        return spec == null || spec.getDerivedStats() == null ? "?" : String.valueOf(Math.round(spec.getDerivedStats().getFluxPerSecond()));
    }

    public String getFluxPerDamageLabel() {
        return spec == null || spec.getDerivedStats() == null ? "?" : formatOneDecimal(spec.getDerivedStats().getFluxPerDam());
    }

    private static String formatOneDecimal(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return "?";
        }
        return String.valueOf(Math.round(value * 10f) / 10f);
    }

    private static String valueOrUnknown(Object value) {
        return value == null ? "?" : String.valueOf(value);
    }
}
