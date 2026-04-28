package weaponinventorymod.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import org.apache.log4j.Logger;
import weaponinventorymod.internal.WeaponInventoryCountUpdater;

import java.util.List;

public class WeaponInventoryModPlugin extends BaseModPlugin {
    private static final Logger LOG = Logger.getLogger(WeaponInventoryModPlugin.class);

    @Override
    public void onGameLoad(boolean newGame) {
        SectorAPI sector = Global.getSector();
        if (sector == null) {
            LOG.warn("WIM_COUNT_UPDATER registration skipped: sector is null");
            return;
        }
        if (hasUpdater(sector.getTransientScripts()) || hasUpdater(sector.getScripts())) {
            return;
        }
        sector.addTransientScript(new WeaponInventoryCountUpdater());
        LOG.info("WIM_COUNT_UPDATER registered");
    }

    private boolean hasUpdater(List<EveryFrameScript> scripts) {
        if (scripts == null) {
            return false;
        }
        for (EveryFrameScript script : scripts) {
            if (script instanceof WeaponInventoryCountUpdater) {
                return true;
            }
        }
        return false;
    }
}
