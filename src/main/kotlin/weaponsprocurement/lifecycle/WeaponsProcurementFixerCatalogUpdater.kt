package weaponsprocurement.lifecycle

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import org.apache.log4j.Logger
import weaponsprocurement.core.FixerMarketObservedCatalog
import weaponsprocurement.config.WeaponMarketBlacklist

class WeaponsProcurementFixerCatalogUpdater : EveryFrameScript {
    private val catalog = FixerMarketObservedCatalog()
    private var lastScanTimestamp = Long.MIN_VALUE
    private var scanErrorLogged = false
    private var scanLogs = 0

    override fun isDone(): Boolean = false

    override fun runWhilePaused(): Boolean = true

    override fun advance(amount: Float) {
        val sector = Global.getSector()
        val clock = sector?.clock ?: return
        if (lastScanTimestamp != Long.MIN_VALUE && clock.getElapsedDaysSince(lastScanTimestamp) < SCAN_INTERVAL_DAYS) {
            return
        }
        lastScanTimestamp = clock.timestamp
        try {
            val added = catalog.observeSectorStock(sector, WeaponMarketBlacklist.load())
            if (added > 0 && scanLogs < MAX_SCAN_LOGS) {
                scanLogs++
                LOG.info("WP_FIXER_CATALOG observed new legal items=$added")
            }
        } catch (t: Throwable) {
            if (!scanErrorLogged) {
                scanErrorLogged = true
                LOG.error("WP_FIXER_CATALOG scan failed", t)
            }
        }
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(WeaponsProcurementFixerCatalogUpdater::class.java)
        private const val SCAN_INTERVAL_DAYS = 1f
        private const val MAX_SCAN_LOGS = 10
    }
}
