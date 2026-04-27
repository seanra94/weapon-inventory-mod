package weaponinventorymod.internal;

import org.apache.log4j.Logger;

public class CargoWeaponMarkerHook {
    private static final Logger LOG = Logger.getLogger(CargoWeaponMarkerHook.class);
    private static boolean logged = false;

    private CargoWeaponMarkerHook() {
    }

    // Legacy diagnostic hook retained as a harmless fallback while direct duplicate-render
    // bytecode diagnostics are active.
    public static void render(float alpha) {
        if (logged) {
            return;
        }
        logged = true;
        LOG.info("WIM_WEAPON_HOOK legacy render(alpha) reached alpha=" + alpha + " (no-op)");
    }
}
