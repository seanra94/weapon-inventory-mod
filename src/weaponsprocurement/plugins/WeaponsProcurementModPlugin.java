package weaponsprocurement.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import org.apache.log4j.Logger;
import weaponsprocurement.internal.WeaponsProcurementFixerCatalogUpdater;
import weaponsprocurement.internal.WeaponsProcurementCountUpdater;
import weaponsprocurement.gui.StockReviewHotkeyScript;

import java.util.List;

public class WeaponsProcurementModPlugin extends BaseModPlugin {
    private static final Logger LOG = Logger.getLogger(WeaponsProcurementModPlugin.class);

    @Override
    public void onGameLoad(boolean newGame) {
        SectorAPI sector = Global.getSector();
        if (sector == null) {
            LOG.warn("WP_COUNT_UPDATER registration skipped: sector is null");
            return;
        }
        if (!hasScript(sector.getTransientScripts(), WeaponsProcurementCountUpdater.class)
                && !hasScript(sector.getScripts(), WeaponsProcurementCountUpdater.class)) {
            sector.addTransientScript(new WeaponsProcurementCountUpdater());
            LOG.info("WP_COUNT_UPDATER registered");
        }
        if (!hasScript(sector.getTransientScripts(), StockReviewHotkeyScript.class)
                && !hasScript(sector.getScripts(), StockReviewHotkeyScript.class)) {
            sector.addTransientScript(new StockReviewHotkeyScript());
            LOG.info("WP_STOCK_REVIEW hotkey registered");
        }
        if (!hasScript(sector.getTransientScripts(), WeaponsProcurementFixerCatalogUpdater.class)
                && !hasScript(sector.getScripts(), WeaponsProcurementFixerCatalogUpdater.class)) {
            sector.addTransientScript(new WeaponsProcurementFixerCatalogUpdater());
            LOG.info("WP_FIXER_CATALOG updater registered");
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
