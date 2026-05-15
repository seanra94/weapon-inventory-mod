package weaponsprocurement.gui;

import com.fs.starfarer.api.combat.WeaponAPI;
import weaponsprocurement.core.WeaponStockRecord;

final class StockReviewRowIcon {
    private final String spriteName;
    private final WeaponAPI.WeaponType motifType;

    private StockReviewRowIcon(String spriteName, WeaponAPI.WeaponType motifType) {
        this.spriteName = spriteName;
        this.motifType = motifType;
    }

    static StockReviewRowIcon weapon(String spriteName, WeaponAPI.WeaponType motifType) {
        if (!WimGuiTooltip.hasText(spriteName)) {
            return null;
        }
        return new StockReviewRowIcon(spriteName, motifType);
    }

    static StockReviewRowIcon weapon(WeaponStockRecord record) {
        if (record == null || record.isWing() || record.getSpec() == null) {
            return null;
        }
        return weapon(StockReviewWeaponIconPlugin.spriteName(record.getSpec()),
                StockReviewWeaponIconPlugin.motifType(record.getSpec()));
    }

    String getSpriteName() {
        return spriteName;
    }

    WeaponAPI.WeaponType getMotifType() {
        return motifType;
    }
}
