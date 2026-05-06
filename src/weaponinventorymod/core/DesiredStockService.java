package weaponinventorymod.core;

import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

public final class DesiredStockService {
    private static final int DEFAULT_SMALL_WEAPON_COUNT = 8;
    private static final int DEFAULT_MEDIUM_WEAPON_COUNT = 4;
    private static final int DEFAULT_LARGE_WEAPON_COUNT = 2;

    public int desiredCount(WeaponSpecAPI spec) {
        if (spec == null || spec.getSize() == null) {
            return DEFAULT_MEDIUM_WEAPON_COUNT;
        }
        WeaponAPI.WeaponSize size = spec.getSize();
        if (WeaponAPI.WeaponSize.SMALL.equals(size)) {
            return DEFAULT_SMALL_WEAPON_COUNT;
        }
        if (WeaponAPI.WeaponSize.MEDIUM.equals(size)) {
            return DEFAULT_MEDIUM_WEAPON_COUNT;
        }
        if (WeaponAPI.WeaponSize.LARGE.equals(size)) {
            return DEFAULT_LARGE_WEAPON_COUNT;
        }
        return DEFAULT_MEDIUM_WEAPON_COUNT;
    }
}
