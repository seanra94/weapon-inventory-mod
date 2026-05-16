package weaponsprocurement.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import org.apache.log4j.Logger;
import weaponsprocurement.internal.WeaponsProcurementFixerCatalogUpdater;
import weaponsprocurement.gui.StockReviewHotkeyScript;

import java.util.List;

public class WeaponsProcurementModPlugin extends BaseModPlugin {
    private static final Logger LOG = Logger.getLogger(WeaponsProcurementModPlugin.class);
    private static final String BADGE_UPDATER_CLASS = "weaponsprocurement.internal.WeaponsProcurementCountUpdater";

    @Override
    public void onGameLoad(boolean newGame) {
        SectorAPI sector = Global.getSector();
        if (sector == null) {
            LOG.warn("WP_PLUGIN registration skipped: sector is null");
            return;
        }
        // PRIVATE_BADGE_START
        registerOptionalPrivateScript(sector, BADGE_UPDATER_CLASS, "WP_COUNT_UPDATER");
        // PRIVATE_BADGE_END
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

    private void registerOptionalPrivateScript(SectorAPI sector, String className, String logName) {
        Class<?> rawClass;
        try {
            rawClass = loadScriptClass(className);
        } catch (ClassNotFoundException ignored) {
            LOG.info(logName + " optional private script not present");
            return;
        }

        if (!EveryFrameScript.class.isAssignableFrom(rawClass)) {
            LOG.warn(logName + " optional private script does not implement EveryFrameScript: " + className);
            return;
        }

        Class<? extends EveryFrameScript> scriptClass = rawClass.asSubclass(EveryFrameScript.class);
        if (hasScript(sector.getTransientScripts(), scriptClass) || hasScript(sector.getScripts(), scriptClass)) {
            return;
        }

        try {
            sector.addTransientScript(scriptClass.getDeclaredConstructor().newInstance());
            LOG.info(logName + " registered");
        } catch (Exception ex) {
            LOG.warn(logName + " optional private script registration failed", ex);
        }
    }

    private Class<?> loadScriptClass(String className) throws ClassNotFoundException {
        if (Global.getSettings() != null && Global.getSettings().getScriptClassLoader() != null) {
            return Global.getSettings().getScriptClassLoader().loadClass(className);
        }
        return Class.forName(className);
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
