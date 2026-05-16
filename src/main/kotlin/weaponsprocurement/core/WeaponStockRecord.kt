package weaponsprocurement.core

import com.fs.starfarer.api.loading.FighterWingSpecAPI
import com.fs.starfarer.api.loading.MissileSpecAPI
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
import java.util.Collections
import java.util.Locale

class WeaponStockRecord(
    itemType: StockItemType?,
    val itemId: String?,
    val displayName: String?,
    val spec: WeaponSpecAPI?,
    val wingSpec: FighterWingSpecAPI?,
    val ownedCount: Int,
    val playerCargoCount: Int,
    val purchasableCount: Int,
    val desiredCount: Int,
    val category: StockCategory?,
    submarketStocks: List<SubmarketWeaponStock>,
    private val fixerRarity: FixerRarity?,
) {
    constructor(
        weaponId: String?,
        displayName: String?,
        spec: WeaponSpecAPI?,
        ownedCount: Int,
        playerCargoCount: Int,
        purchasableCount: Int,
        desiredCount: Int,
        category: StockCategory?,
        submarketStocks: List<SubmarketWeaponStock>,
    ) : this(
        StockItemType.WEAPON,
        weaponId,
        displayName,
        spec,
        null,
        ownedCount,
        playerCargoCount,
        purchasableCount,
        desiredCount,
        category,
        submarketStocks,
        null,
    )

    constructor(
        itemType: StockItemType?,
        itemId: String?,
        displayName: String?,
        spec: WeaponSpecAPI?,
        wingSpec: FighterWingSpecAPI?,
        ownedCount: Int,
        playerCargoCount: Int,
        purchasableCount: Int,
        desiredCount: Int,
        category: StockCategory?,
        submarketStocks: List<SubmarketWeaponStock>,
    ) : this(
        itemType,
        itemId,
        displayName,
        spec,
        wingSpec,
        ownedCount,
        playerCargoCount,
        purchasableCount,
        desiredCount,
        category,
        submarketStocks,
        null,
    )

    val itemType: StockItemType = itemType ?: StockItemType.WEAPON
    val itemKey: String = this.itemType.key(itemId)
    val submarketStocks: List<SubmarketWeaponStock> = Collections.unmodifiableList(submarketStocks)

    fun isWing(): Boolean = StockItemType.WING == itemType

    val storageCount: Int
        get() = ownedCount

    val buyableCount: Int
        get() {
            var count = 0
            for (stock in submarketStocks) {
                if (stock.isPurchasable()) count += stock.count
            }
            return count
        }

    val neededCount: Int
        get() = Math.max(0, desiredCount - ownedCount)

    val cheapestPurchasableUnitPrice: Int
        get() {
            var cheapest = Int.MAX_VALUE
            for (stock in submarketStocks) {
                if (!stock.isPurchasable() || stock.count <= 0) continue
                cheapest = Math.min(cheapest, stock.unitPrice)
            }
            return cheapest
        }

    val fixerRarityLabel: String?
        get() = fixerRarity?.label

    val sizeLabel: String
        get() {
            if (isWing()) return "Wing"
            return valueOrUnknown(spec?.size)
        }

    val typeLabel: String
        get() {
            if (isWing()) return valueOrUnknown(wingSpec?.role)
            return valueOrUnknown(spec?.type)
        }

    val primaryRoleLabel: String
        get() = valueOrUnknown(spec?.primaryRoleStr)

    val opCostLabel: String
        get() = if (spec == null) "?" else Math.round(spec.getOrdnancePointCost(null)).toString()

    val damageTypeLabel: String
        get() = valueOrUnknown(spec?.damageType)

    val damageLabel: String
        get() {
            if (isWing()) return "?"
            return if (spec?.derivedStats == null) "?" else Math.round(spec.derivedStats.damagePerShot).toString()
        }

    val empLabel: String
        get() {
            if (isWing()) return "?"
            return if (spec?.derivedStats == null) "?" else Math.round(spec.derivedStats.empPerShot).toString()
        }

    val rangeLabel: String
        get() {
            if (isWing()) return if (wingSpec == null) "?" else Math.round(wingSpec.range).toString()
            return if (spec == null) "?" else Math.round(spec.maxRange).toString()
        }

    val fluxPerSecondLabel: String
        get() = if (isWing() || spec?.derivedStats == null) {
            "?"
        } else {
            Math.round(spec.derivedStats.fluxPerSecond).toString()
        }

    val fluxPerDamageLabel: String
        get() = if (isWing() || spec?.derivedStats == null) {
            "?"
        } else {
            formatOneDecimal(spec.derivedStats.fluxPerDam)
        }

    val refireSecondsLabel: String
        get() {
            val projectile = projectileWeaponSpec()
            return if (projectile == null) "?" else formatTwoDecimals(projectile.refireDelay)
        }

    val sustainedDamagePerSecondLabel: String
        get() {
            if (spec?.derivedStats == null) return "?"
            val sustained = Math.round(spec.derivedStats.sustainedDps)
            val burst = Math.round(spec.derivedStats.dps)
            return if (sustained == burst) sustained.toString() else "$sustained ($burst)"
        }

    fun hasDifferentSustainedDamagePerSecond(): Boolean {
        return spec?.derivedStats != null &&
            Math.round(spec.derivedStats.sustainedDps) != Math.round(spec.derivedStats.dps)
    }

    val sustainedFluxPerSecondLabel: String
        get() {
            if (spec?.derivedStats == null) return "?"
            val sustained = Math.round(spec.derivedStats.sustainedFluxPerSecond)
            val burst = Math.round(spec.derivedStats.fluxPerSecond)
            return if (sustained == burst) sustained.toString() else "$sustained ($burst)"
        }

    fun hasDifferentSustainedFluxPerSecond(): Boolean {
        return spec?.derivedStats != null &&
            Math.round(spec.derivedStats.sustainedFluxPerSecond) != Math.round(spec.derivedStats.fluxPerSecond)
    }

    val sustainedEmpPerSecondLabel: String
        get() = if (spec?.derivedStats == null) "?" else Math.round(spec.derivedStats.empPerSecond).toString()

    fun hasDifferentSustainedEmpPerSecond(): Boolean = false

    val fluxPerEmpLabel: String
        get() {
            val stats = spec?.derivedStats ?: return "?"
            if (stats.empPerSecond <= 0f) return "?"
            return Math.round(stats.sustainedFluxPerSecond / stats.empPerSecond).toString()
        }

    val beamDpsLabel: String
        get() = if (spec == null || !spec.isBeam || spec.derivedStats == null) {
            "?"
        } else {
            Math.round(spec.derivedStats.dps).toString()
        }

    val beamChargeUpLabel: String
        get() = if (spec == null || spec.beamChargeupTime <= 0f) "?" else formatTwoDecimals(spec.beamChargeupTime)

    val beamChargeDownLabel: String
        get() = if (spec == null || spec.beamChargedownTime <= 0f) "?" else formatTwoDecimals(spec.beamChargedownTime)

    val burstDelayLabel: String
        get() {
            val projectile = projectileWeaponSpec()
            return if (projectile == null || projectile.burstDelay <= 0f) "?" else formatTwoDecimals(projectile.burstDelay)
        }

    val turnRateLabel: String
        get() {
            if (spec == null || spec.turnRate <= 0f) return "?"
            return Math.round(spec.turnRate).toString() + "\u00b0/s"
        }

    val minSpreadLabel: String
        get() = if (spec == null || spec.minSpread <= 0f) "?" else formatOneDecimal(spec.minSpread)

    val maxSpreadLabel: String
        get() = if (spec == null || spec.maxSpread <= 0f) "?" else formatOneDecimal(spec.maxSpread)

    val spreadPerShotLabel: String
        get() = if (spec == null || spec.spreadBuildup <= 0f) "?" else formatOneDecimal(spec.spreadBuildup)

    val spreadDecayLabel: String
        get() = if (spec == null || spec.spreadDecayRate <= 0f) "?" else formatOneDecimal(spec.spreadDecayRate)

    val projectileSpeedLabel: String
        get() {
            val projectile = projectileWeaponSpec() ?: return "?"
            val speed = try {
                projectile.getProjectileSpeed(null, null)
            } catch (_: RuntimeException) {
                return "?"
            }
            return if (speed <= 0f || speed.isNaN() || speed.isInfinite()) "?" else Math.round(speed).toString()
        }

    val launchSpeedLabel: String
        get() {
            val missile = missileSpec()
            return if (missile == null || missile.launchSpeed <= 0f) "?" else Math.round(missile.launchSpeed).toString()
        }

    val flightTimeLabel: String
        get() {
            val missile = missileSpec()
            return if (missile == null || missile.maxFlightTime <= 0f) "?" else formatTwoDecimals(missile.maxFlightTime)
        }

    val guidedLabel: String
        get() {
            if (missileSpec() == null) return "?"
            val tracking = spec?.trackingStr
            val guided = !tracking.isNullOrBlank() && !"None".equals(tracking.trim(), ignoreCase = true)
            return if (guided) "TRUE" else "FALSE"
        }

    val maxAmmoLabel: String
        get() = if (spec == null || !spec.usesAmmo() || spec.maxAmmo <= 0) "?" else spec.maxAmmo.toString()

    val secPerReloadLabel: String
        get() {
            if (spec == null || !spec.usesAmmo() || spec.ammoPerSecond <= 0f || spec.reloadSize <= 0f) return "?"
            return formatTwoDecimals(spec.reloadSize / spec.ammoPerSecond)
        }

    val ammoGainLabel: String
        get() = if (spec == null || !spec.usesAmmo() || spec.ammoPerSecond <= 0f) {
            "?"
        } else {
            formatOneDecimal(spec.ammoPerSecond)
        }

    val accuracyLabel: String
        get() = valueOrUnknown(spec?.accuracyStr)

    val wingFighterCountLabel: String
        get() = wingSpec?.numFighters?.toString() ?: "?"

    val wingOpCostLabel: String
        get() = if (wingSpec == null) "?" else Math.round(wingSpec.getOpCost(null)).toString()

    val wingRefitTimeLabel: String
        get() = if (wingSpec == null) "?" else formatOneDecimal(wingSpec.refitTime) + "s"

    private fun projectileWeaponSpec(): ProjectileWeaponSpecAPI? {
        return if (spec is ProjectileWeaponSpecAPI) spec else null
    }

    private fun missileSpec(): MissileSpecAPI? {
        val projectile = spec?.projectileSpec
        return if (projectile is MissileSpecAPI) projectile else null
    }

    companion object {
        private fun formatOneDecimal(value: Float): String {
            if (value.isNaN() || value.isInfinite()) return "?"
            return (Math.round(value * 10f) / 10f).toString()
        }

        private fun formatTwoDecimals(value: Float): String {
            if (value.isNaN() || value.isInfinite()) return "?"
            return String.format(Locale.US, "%.2f", value)
        }

        private fun valueOrUnknown(value: Any?): String {
            return value?.toString() ?: "?"
        }
    }
}
