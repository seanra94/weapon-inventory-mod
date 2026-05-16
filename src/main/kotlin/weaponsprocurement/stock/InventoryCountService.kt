package weaponsprocurement.stock

import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.util.Misc
import java.util.HashMap

class InventoryCountService {
    fun collectOwnedItemCounts(
        sector: SectorAPI?,
        market: MarketAPI?,
        policy: OwnedSourcePolicy?,
    ): Map<String, Int> {
        val counts = collectOwnedCounts(sector, market, policy, StockItemType.WEAPON)
        merge(counts, collectOwnedCounts(sector, market, policy, StockItemType.WING))
        return counts
    }

    private fun collectOwnedCounts(
        sector: SectorAPI?,
        market: MarketAPI?,
        policy: OwnedSourcePolicy?,
        itemType: StockItemType,
    ): MutableMap<String, Int> {
        val counts = HashMap<String, Int>()
        if (sector == null) return counts

        val fleet = sector.playerFleet
        merge(counts, collectCargoCounts(fleet?.cargo, itemType))

        if (OwnedSourcePolicy.FLEET_AND_ACCESSIBLE_STORAGE == policy) {
            mergeAccessibleStorage(counts, sector, itemType)
        } else if (
            OwnedSourcePolicy.FLEET_AND_CURRENT_MARKET_STORAGE == policy &&
            market != null &&
            Misc.playerHasStorageAccess(market)
        ) {
            merge(counts, collectCargoCounts(Misc.getStorageCargo(market), itemType))
        }

        return counts
    }

    companion object {
        private fun mergeAccessibleStorage(counts: MutableMap<String, Int>, sector: SectorAPI, itemType: StockItemType) {
            val economy = sector.economy
            val markets = economy?.marketsCopy ?: return
            for (storageMarket in markets) {
                if (storageMarket == null || !Misc.playerHasStorageAccess(storageMarket)) continue
                merge(counts, collectCargoCounts(Misc.getStorageCargo(storageMarket), itemType))
            }
        }

        @JvmStatic
        fun collectCargoWeaponCounts(cargo: CargoAPI?): MutableMap<String, Int> {
            val counts = HashMap<String, Int>()
            val weapons = cargo?.weapons ?: return counts
            for (quantity in weapons) {
                if (quantity != null) add(counts, quantity.item, quantity.count)
            }
            return counts
        }

        @JvmStatic
        fun collectCargoFighterCounts(cargo: CargoAPI?): MutableMap<String, Int> {
            val counts = HashMap<String, Int>()
            val fighters = cargo?.fighters ?: return counts
            for (quantity in fighters) {
                if (quantity != null) add(counts, quantity.item, quantity.count)
            }
            return counts
        }

        @JvmStatic
        fun collectCargoItemCounts(cargo: CargoAPI?): MutableMap<String, Int> {
            val counts = HashMap<String, Int>()
            mergeWithPrefix(counts, collectCargoWeaponCounts(cargo), StockItemType.WEAPON)
            mergeWithPrefix(counts, collectCargoFighterCounts(cargo), StockItemType.WING)
            return counts
        }

        private fun collectCargoCounts(cargo: CargoAPI?, itemType: StockItemType): MutableMap<String, Int> {
            val raw = if (StockItemType.WING == itemType) {
                collectCargoFighterCounts(cargo)
            } else {
                collectCargoWeaponCounts(cargo)
            }
            val keyed = HashMap<String, Int>()
            mergeWithPrefix(keyed, raw, itemType)
            return keyed
        }

        @JvmStatic
        fun merge(target: MutableMap<String, Int>, source: Map<String, Int>) {
            for (entry in source.entries) {
                add(target, entry.key, entry.value)
            }
        }

        @JvmStatic
        fun add(counts: MutableMap<String, Int>, id: String?, count: Int) {
            if (id.isNullOrEmpty() || count == 0) return
            val existing = counts[id] ?: 0
            counts[id] = existing + count
        }

        private fun mergeWithPrefix(target: MutableMap<String, Int>, source: Map<String, Int>, itemType: StockItemType) {
            for (entry in source.entries) {
                add(target, itemType.key(entry.key), entry.value)
            }
        }
    }
}
