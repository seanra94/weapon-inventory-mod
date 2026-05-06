package weaponinventorymod.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import org.apache.log4j.Logger;
import weaponinventorymod.internal.WeaponInventoryCountUpdater;
import weaponinventorymod.gui.StockReviewHotkeyScript;

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
        if (!hasScript(sector.getTransientScripts(), WeaponInventoryCountUpdater.class)
                && !hasScript(sector.getScripts(), WeaponInventoryCountUpdater.class)) {
            sector.addTransientScript(new WeaponInventoryCountUpdater());
            LOG.info("WIM_COUNT_UPDATER registered");
        }
        if (!hasScript(sector.getTransientScripts(), StockReviewHotkeyScript.class)
                && !hasScript(sector.getScripts(), StockReviewHotkeyScript.class)) {
            sector.addTransientScript(new StockReviewHotkeyScript());
            LOG.info("WIM_STOCK_REVIEW hotkey registered");
        }
    }

    private boolean hasScript(List<EveryFrameScript> scripts, Class<? extends EveryFrameScript> scriptClass) {
        if (scripts == null) {
            return false;
        }
        for (EveryFrameScript script : scripts) {
            if (scriptClass.isInstance(script)) {
                return true;
            }
        }
        return false;
    }
}
