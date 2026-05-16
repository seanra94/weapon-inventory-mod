package weaponsprocurement.plugins

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorAPI
import org.apache.log4j.Logger
import weaponsprocurement.gui.StockReviewHotkeyScript
import weaponsprocurement.internal.WeaponsProcurementFixerCatalogUpdater

class WeaponsProcurementModPlugin : BaseModPlugin() {
    override fun onGameLoad(newGame: Boolean) {
        val sector = Global.getSector()
        if (sector == null) {
            LOG.warn("WP_PLUGIN registration skipped: sector is null")
            return
        }
        // PRIVATE_BADGE_START
        registerOptionalPrivateScript(sector, BADGE_UPDATER_CLASS, "WP_COUNT_UPDATER")
        // PRIVATE_BADGE_END
        if (!hasScript(sector.transientScripts, StockReviewHotkeyScript::class.java) &&
            !hasScript(sector.scripts, StockReviewHotkeyScript::class.java)
        ) {
            sector.addTransientScript(StockReviewHotkeyScript())
            LOG.info("WP_STOCK_REVIEW hotkey registered")
        }
        if (!hasScript(sector.transientScripts, WeaponsProcurementFixerCatalogUpdater::class.java) &&
            !hasScript(sector.scripts, WeaponsProcurementFixerCatalogUpdater::class.java)
        ) {
            sector.addTransientScript(WeaponsProcurementFixerCatalogUpdater())
            LOG.info("WP_FIXER_CATALOG updater registered")
        }
    }

    private fun registerOptionalPrivateScript(sector: SectorAPI, className: String, logName: String) {
        val rawClass = try {
            loadScriptClass(className)
        } catch (_: ClassNotFoundException) {
            LOG.info("$logName optional private script not present")
            return
        }

        if (!EveryFrameScript::class.java.isAssignableFrom(rawClass)) {
            LOG.warn("$logName optional private script does not implement EveryFrameScript: $className")
            return
        }

        val scriptClass = rawClass.asSubclass(EveryFrameScript::class.java)
        if (hasScript(sector.transientScripts, scriptClass) || hasScript(sector.scripts, scriptClass)) {
            return
        }

        try {
            sector.addTransientScript(scriptClass.getDeclaredConstructor().newInstance())
            LOG.info("$logName registered")
        } catch (ex: Exception) {
            LOG.warn("$logName optional private script registration failed", ex)
        }
    }

    @Throws(ClassNotFoundException::class)
    private fun loadScriptClass(className: String): Class<*> {
        val settings = Global.getSettings()
        if (settings != null && settings.scriptClassLoader != null) {
            return settings.scriptClassLoader.loadClass(className)
        }
        return Class.forName(className)
    }

    private fun hasScript(scripts: List<EveryFrameScript>?, scriptClass: Class<out EveryFrameScript>): Boolean {
        if (scripts == null) {
            return false
        }
        for (script in scripts) {
            if (scriptClass.isInstance(script)) {
                return true
            }
        }
        return false
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(WeaponsProcurementModPlugin::class.java)
        private const val BADGE_UPDATER_CLASS = "weaponsprocurement.internal.WeaponsProcurementCountUpdater"
    }
}
