package weaponinventorymod.core;

import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

public final class DesiredStockService {
    private final StockReviewConfig config;

    public DesiredStockService(StockReviewConfig config) {
        this.config = config;
    }

    public int desiredCount(String weaponId, WeaponSpecAPI spec) {
        if (spec == null || spec.getSize() == null) {
            return config.desiredCount(weaponId, WeaponAPI.WeaponSize.MEDIUM);
        }
        return config.desiredCount(weaponId, spec.getSize());
    }
}
