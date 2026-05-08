package weaponinventorymod.core;

import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
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

    public String getWeaponId() {
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
        if (isWing()) {
            int op = wingSpec == null ? 0 : Math.round(wingSpec.getOpCost(null));
            int range = wingSpec == null ? 0 : Math.round(wingSpec.getRange());
            return "Desired: " + desiredCount
                    + " | Size: Wing"
                    + " | Role: " + valueOrUnknown(wingSpec == null ? null : wingSpec.getRole())
                    + " | OP: " + op
                    + " | Range: " + range;
        }
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

    private static String valueOrUnknown(Object value) {
        return value == null ? "?" : String.valueOf(value);
    }
}
