package weaponsprocurement.internal;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import org.apache.log4j.Logger;
import weaponsprocurement.core.FixerMarketObservedCatalog;
import weaponsprocurement.core.WeaponMarketBlacklist;

public class WeaponsProcurementFixerCatalogUpdater implements EveryFrameScript {
    private static final Logger LOG = Logger.getLogger(WeaponsProcurementFixerCatalogUpdater.class);
    private static final float SCAN_INTERVAL_DAYS = 1f;
    private static final int MAX_SCAN_LOGS = 10;

    private final FixerMarketObservedCatalog catalog = new FixerMarketObservedCatalog();

    private long lastScanTimestamp = Long.MIN_VALUE;
    private boolean scanErrorLogged = false;
    private int scanLogs = 0;

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        SectorAPI sector = Global.getSector();
        CampaignClockAPI clock = sector == null ? null : sector.getClock();
        if (sector == null || clock == null) {
            return;
        }
        if (lastScanTimestamp != Long.MIN_VALUE
                && clock.getElapsedDaysSince(lastScanTimestamp) < SCAN_INTERVAL_DAYS) {
            return;
        }
        lastScanTimestamp = clock.getTimestamp();
        try {
            int added = catalog.observeSectorStock(sector, WeaponMarketBlacklist.load());
            if (added > 0 && scanLogs < MAX_SCAN_LOGS) {
                scanLogs++;
                LOG.info("WP_FIXER_CATALOG observed new legal items=" + added);
            }
        } catch (Throwable t) {
            if (!scanErrorLogged) {
                scanErrorLogged = true;
                LOG.error("WP_FIXER_CATALOG scan failed", t);
            }
        }
    }
}
