package weaponinventorymod.internal;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

public class WeaponInventoryBadgeHelper {
    private static final String[] DIGIT_BADGES = new String[]{
            null,
            "graphics/ui/wim_badge_1.png",
            "graphics/ui/wim_badge_2.png",
            "graphics/ui/wim_badge_3.png",
            "graphics/ui/wim_badge_4.png",
            "graphics/ui/wim_badge_5.png",
            "graphics/ui/wim_badge_6.png",
            "graphics/ui/wim_badge_7.png",
            "graphics/ui/wim_badge_8.png",
            "graphics/ui/wim_badge_9.png"
    };
    private static final String CLAMP_BADGE = "graphics/ui/wim_badge_9plus.png";

    private WeaponInventoryBadgeHelper() {
    }

    public static String getBadgeSpritePath(String weaponId) {
        if (weaponId == null || weaponId.isEmpty()) {
            return null;
        }
        int count = getPlayerFleetWeaponCount(weaponId);
        if (count <= 0) {
            return null;
        }
        if (count >= 10) {
            return CLAMP_BADGE;
        }
        return DIGIT_BADGES[count];
    }

    private static int getPlayerFleetWeaponCount(String weaponId) {
        try {
            SectorAPI sector = Global.getSector();
            if (sector == null) {
                return 0;
            }
            CampaignFleetAPI fleet = sector.getPlayerFleet();
            if (fleet == null) {
                return 0;
            }
            CargoAPI cargo = fleet.getCargo();
            if (cargo == null) {
                return 0;
            }
            return cargo.getNumWeapons(weaponId);
        } catch (Throwable ignored) {
            return 0;
        }
    }
}
