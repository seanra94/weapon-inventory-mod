package weaponsprocurement.core;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MarketStockService {
    public MarketStock collectCurrentMarketItemStock(MarketAPI market, boolean includeBlackMarket) {
        MarketStockBuilder builder = new MarketStockBuilder();
        builder.addAll(collectCurrentMarketStock(market, includeBlackMarket, StockItemType.WEAPON));
        builder.addAll(collectCurrentMarketStock(market, includeBlackMarket, StockItemType.WING));
        return builder.build();
    }

    private MarketStock collectCurrentMarketStock(MarketAPI market, boolean includeBlackMarket, StockItemType itemType) {
        Map<String, Integer> totals = new HashMap<String, Integer>();
        Map<String, List<SubmarketWeaponStock>> byItemKey = new HashMap<String, List<SubmarketWeaponStock>>();
        if (market == null || market.getSubmarketsCopy() == null) {
            return new MarketStock(totals, byItemKey);
        }

        for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
            if (!isTradeSubmarket(submarket, includeBlackMarket)) {
                continue;
            }
            CargoAPI cargo = submarket.getCargoNullOk();
            if (cargo == null || cargo.getStacksCopy() == null) {
                continue;
            }
            for (CargoStackAPI stack : cargo.getStacksCopy()) {
                if (!StockItemStacks.isVisibleItemStack(stack, itemType)) {
                    continue;
                }
                String itemKey = itemType.key(StockItemStacks.itemId(stack, itemType));
                int count = Math.round(stack.getSize());
                if (count <= 0) {
                    continue;
                }
                InventoryCountService.add(totals, itemKey, count);
                List<SubmarketWeaponStock> stocks = byItemKey.get(itemKey);
                if (stocks == null) {
                    stocks = new ArrayList<SubmarketWeaponStock>();
                    byItemKey.put(itemKey, stocks);
                }
                stocks.add(new SubmarketWeaponStock(
                        market.getId(),
                        market.getName(),
                        submarket.getSpecId(),
                        submarket.getNameOneLine(),
                        count,
                        StockItemStacks.unitPrice(submarket, stack),
                        StockItemStacks.baseUnitPrice(stack),
                        StockItemStacks.unitCargoSpace(stack),
                        StockItemStacks.isPurchasableItemStack(submarket, stack, itemType)));
            }
        }

        return new MarketStock(totals, byItemKey);
    }

    public static boolean isTradeSubmarket(SubmarketAPI submarket, boolean includeBlackMarket) {
        if (submarket == null) {
            return false;
        }
        String id = submarket.getSpecId();
        if (isNonTradeSubmarket(id)) {
            return false;
        }
        if (!includeBlackMarket && Submarkets.SUBMARKET_BLACK.equals(id)) {
            return false;
        }
        return submarket.getCargoNullOk() != null;
    }

    public static boolean isNonTradeSubmarket(String submarketId) {
        return Submarkets.SUBMARKET_STORAGE.equals(submarketId) || Submarkets.LOCAL_RESOURCES.equals(submarketId);
    }

    public static boolean isBlackMarketSubmarket(String submarketId) {
        return Submarkets.SUBMARKET_BLACK.equals(submarketId);
    }

    public static final class MarketStock {
        private final Map<String, Integer> totals;
        private final Map<String, List<SubmarketWeaponStock>> byItemKey;

        private MarketStock(Map<String, Integer> totals, Map<String, List<SubmarketWeaponStock>> byItemKey) {
            this.totals = totals;
            this.byItemKey = byItemKey;
        }

        public int getTotal(String itemKey) {
            Integer count = totals.get(itemKey);
            return count == null ? 0 : count.intValue();
        }

        public List<SubmarketWeaponStock> getSubmarketStocks(String itemKey) {
            List<SubmarketWeaponStock> stocks = byItemKey.get(itemKey);
            if (stocks == null) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(stocks);
        }

        public Iterable<String> itemKeys() {
            return totals.keySet();
        }
    }

    public static final class MarketStockBuilder {
        private final Map<String, Integer> totals = new HashMap<String, Integer>();
        private final Map<String, List<SubmarketWeaponStock>> byItemKey = new HashMap<String, List<SubmarketWeaponStock>>();

        public void add(String itemKey, SubmarketWeaponStock stock) {
            if (itemKey == null || itemKey.isEmpty() || stock == null || stock.getCount() <= 0) {
                return;
            }
            InventoryCountService.add(totals, itemKey, stock.getCount());
            List<SubmarketWeaponStock> stocks = byItemKey.get(itemKey);
            if (stocks == null) {
                stocks = new ArrayList<SubmarketWeaponStock>();
                byItemKey.put(itemKey, stocks);
            }
            stocks.add(stock);
        }

        public void addAll(MarketStock stock) {
            if (stock == null) {
                return;
            }
            for (String id : stock.itemKeys()) {
                List<SubmarketWeaponStock> sources = stock.getSubmarketStocks(id);
                for (int i = 0; i < sources.size(); i++) {
                    add(id, sources.get(i));
                }
            }
        }

        public MarketStock build() {
            return new MarketStock(totals, byItemKey);
        }
    }
}
