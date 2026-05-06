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
    private final int purchasableCount;
    private final int desiredCount;
    private final StockCategory category;
    private final List<SubmarketWeaponStock> submarketStocks;

    public WeaponStockRecord(String weaponId,
                             String displayName,
                             WeaponSpecAPI spec,
                             int ownedCount,
                             int purchasableCount,
                             int desiredCount,
                             StockCategory category,
                             List<SubmarketWeaponStock> submarketStocks) {
        this.weaponId = weaponId;
        this.displayName = displayName;
        this.spec = spec;
        this.ownedCount = ownedCount;
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

    public int getPurchasableCount() {
        return purchasableCount;
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

    private static String valueOrUnknown(Object value) {
        return value == null ? "?" : String.valueOf(value);
    }
}
