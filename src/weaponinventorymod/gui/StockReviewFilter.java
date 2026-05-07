package weaponinventorymod.gui;

import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.WeaponAPI;
import weaponinventorymod.core.WeaponStockRecord;

enum StockReviewFilter {
    SIZE_SMALL(StockReviewFilterGroup.SIZE, "Small"),
    SIZE_MEDIUM(StockReviewFilterGroup.SIZE, "Medium"),
    SIZE_LARGE(StockReviewFilterGroup.SIZE, "Large"),
    TYPE_BALLISTIC(StockReviewFilterGroup.TYPE, "Ballistic"),
    TYPE_ENERGY(StockReviewFilterGroup.TYPE, "Energy"),
    TYPE_MISSILE(StockReviewFilterGroup.TYPE, "Missile"),
    DAMAGE_KINETIC(StockReviewFilterGroup.DAMAGE, "Kinetic"),
    DAMAGE_HIGH_EXPLOSIVE(StockReviewFilterGroup.DAMAGE, "High Explosive"),
    DAMAGE_ENERGY(StockReviewFilterGroup.DAMAGE, "Energy"),
    DAMAGE_FRAGMENTATION(StockReviewFilterGroup.DAMAGE, "Fragmentation");

    private final StockReviewFilterGroup group;
    private final String label;

    StockReviewFilter(StockReviewFilterGroup group, String label) {
        this.group = group;
        this.label = label;
    }

    StockReviewFilterGroup getGroup() {
        return group;
    }

    String getLabel() {
        return label;
    }

    boolean matches(WeaponStockRecord record) {
        if (record == null || record.getSpec() == null) {
            return false;
        }
        if (this == SIZE_SMALL) {
            return WeaponAPI.WeaponSize.SMALL.equals(record.getSpec().getSize());
        }
        if (this == SIZE_MEDIUM) {
            return WeaponAPI.WeaponSize.MEDIUM.equals(record.getSpec().getSize());
        }
        if (this == SIZE_LARGE) {
            return WeaponAPI.WeaponSize.LARGE.equals(record.getSpec().getSize());
        }
        if (this == TYPE_BALLISTIC) {
            return WeaponAPI.WeaponType.BALLISTIC.equals(record.getSpec().getType());
        }
        if (this == TYPE_ENERGY) {
            return WeaponAPI.WeaponType.ENERGY.equals(record.getSpec().getType());
        }
        if (this == TYPE_MISSILE) {
            return WeaponAPI.WeaponType.MISSILE.equals(record.getSpec().getType());
        }
        if (this == DAMAGE_KINETIC) {
            return DamageType.KINETIC.equals(record.getSpec().getDamageType());
        }
        if (this == DAMAGE_HIGH_EXPLOSIVE) {
            return DamageType.HIGH_EXPLOSIVE.equals(record.getSpec().getDamageType());
        }
        if (this == DAMAGE_ENERGY) {
            return DamageType.ENERGY.equals(record.getSpec().getDamageType());
        }
        if (this == DAMAGE_FRAGMENTATION) {
            return DamageType.FRAGMENTATION.equals(record.getSpec().getDamageType());
        }
        return false;
    }
}
