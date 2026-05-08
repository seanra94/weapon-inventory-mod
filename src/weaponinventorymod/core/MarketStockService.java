package weaponinventorymod.core;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MarketStockService {
    public MarketStock collectCurrentMarketWeaponStock(MarketAPI market, boolean includeBlackMarket) {
        return collectCurrentMarketStock(market, includeBlackMarket, StockItemType.WEAPON);
    }

    public MarketStock collectCurrentMarketWingStock(MarketAPI market, boolean includeBlackMarket) {
        return collectCurrentMarketStock(market, includeBlackMarket, StockItemType.WING);
    }

    public MarketStock collectCurrentMarketItemStock(MarketAPI market, boolean includeBlackMarket) {
        MarketStockBuilder builder = new MarketStockBuilder();
        builder.addAll(collectCurrentMarketStock(market, includeBlackMarket, StockItemType.WEAPON));
        builder.addAll(collectCurrentMarketStock(market, includeBlackMarket, StockItemType.WING));
        return builder.build();
    }

    private MarketStock collectCurrentMarketStock(MarketAPI market, boolean includeBlackMarket, StockItemType itemType) {
        Map<String, Integer> totals = new HashMap<String, Integer>();
        Map<String, List<SubmarketWeaponStock>> byWeapon = new HashMap<String, List<SubmarketWeaponStock>>();
        if (market == null || market.getSubmarketsCopy() == null) {
            return new MarketStock(totals, byWeapon);
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
                if (!isVisibleItemStack(stack, itemType)) {
                    continue;
                }
                String weaponId = itemType.key(itemId(stack, itemType));
                int count = Math.round(stack.getSize());
                if (count <= 0) {
                    continue;
                }
                InventoryCountService.add(totals, weaponId, count);
                List<SubmarketWeaponStock> stocks = byWeapon.get(weaponId);
                if (stocks == null) {
                    stocks = new ArrayList<SubmarketWeaponStock>();
                    byWeapon.put(weaponId, stocks);
                }
                stocks.add(new SubmarketWeaponStock(
                        market.getId(),
                        market.getName(),
                        submarket.getSpecId(),
                        submarket.getNameOneLine(),
                        count,
                        unitPrice(submarket, stack),
                        baseUnitPrice(stack),
                        unitCargoSpace(stack),
                        isPurchasableItemStack(submarket, stack, itemType)));
            }
        }

        return new MarketStock(totals, byWeapon);
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

    public static boolean isVisibleWeaponStack(CargoStackAPI stack) {
        return stack != null && stack.isWeaponStack() && stack.getWeaponSpecIfWeapon() != null && stack.getSize() > 0f;
    }

    public static boolean isVisibleWingStack(CargoStackAPI stack) {
        return stack != null && stack.isFighterWingStack() && stack.getFighterWingSpecIfWing() != null && stack.getSize() > 0f;
    }

    public static boolean isVisibleItemStack(CargoStackAPI stack, StockItemType itemType) {
        return StockItemType.WING.equals(itemType) ? isVisibleWingStack(stack) : isVisibleWeaponStack(stack);
    }

    public static boolean isPurchasableWeaponStack(SubmarketAPI submarket, CargoStackAPI stack) {
        if (submarket == null || stack == null || !stack.isWeaponStack() || stack.getWeaponSpecIfWeapon() == null) {
            return false;
        }
        SubmarketPlugin plugin = submarket.getPlugin();
        if (plugin != null && plugin.isIllegalOnSubmarket(stack, SubmarketPlugin.TransferAction.PLAYER_BUY)) {
            return false;
        }
        return stack.getSize() > 0f;
    }

    public static boolean isPurchasableWingStack(SubmarketAPI submarket, CargoStackAPI stack) {
        if (submarket == null || stack == null || !stack.isFighterWingStack() || stack.getFighterWingSpecIfWing() == null) {
            return false;
        }
        SubmarketPlugin plugin = submarket.getPlugin();
        if (plugin != null && plugin.isIllegalOnSubmarket(stack, SubmarketPlugin.TransferAction.PLAYER_BUY)) {
            return false;
        }
        return stack.getSize() > 0f;
    }

    public static boolean isPurchasableItemStack(SubmarketAPI submarket, CargoStackAPI stack, StockItemType itemType) {
        return StockItemType.WING.equals(itemType)
                ? isPurchasableWingStack(submarket, stack)
                : isPurchasableWeaponStack(submarket, stack);
    }

    public static String itemId(CargoStackAPI stack, StockItemType itemType) {
        if (stack == null) {
            return null;
        }
        if (StockItemType.WING.equals(itemType)) {
            return stack.getFighterWingSpecIfWing() == null ? null : stack.getFighterWingSpecIfWing().getId();
        }
        return stack.getWeaponSpecIfWeapon() == null ? null : stack.getWeaponSpecIfWeapon().getWeaponId();
    }

    public static int unitPrice(SubmarketAPI submarket, CargoStackAPI stack) {
        if (stack == null) {
            return 0;
        }
        float tariff = 0f;
        if (submarket != null) {
            SubmarketPlugin plugin = submarket.getPlugin();
            tariff = plugin == null ? submarket.getTariff() : plugin.getTariff();
        }
        return Math.max(0, Math.round(stack.getBaseValuePerUnit() * (1f + Math.max(0f, tariff))));
    }

    public static int baseUnitPrice(CargoStackAPI stack) {
        if (stack == null) {
            return 0;
        }
        return Math.max(0, Math.round(stack.getBaseValuePerUnit()));
    }

    public static int sellUnitPrice(SubmarketAPI submarket, CargoStackAPI stack) {
        if (stack == null) {
            return 0;
        }
        float tariff = 0f;
        if (submarket != null) {
            SubmarketPlugin plugin = submarket.getPlugin();
            tariff = plugin == null ? submarket.getTariff() : plugin.getTariff();
        }
        return Math.max(0, Math.round(stack.getBaseValuePerUnit() * (1f - Math.max(0f, tariff))));
    }

    public static float unitCargoSpace(CargoStackAPI stack) {
        if (stack == null) {
            return 1f;
        }
        float value = stack.getCargoSpacePerUnit();
        return value <= 0f ? 1f : value;
    }

    public static final class MarketStock {
        private final Map<String, Integer> totals;
        private final Map<String, List<SubmarketWeaponStock>> byWeapon;

        private MarketStock(Map<String, Integer> totals, Map<String, List<SubmarketWeaponStock>> byWeapon) {
            this.totals = totals;
            this.byWeapon = byWeapon;
        }

        public int getTotal(String weaponId) {
            Integer count = totals.get(weaponId);
            return count == null ? 0 : count.intValue();
        }

        public List<SubmarketWeaponStock> getSubmarketStocks(String weaponId) {
            List<SubmarketWeaponStock> stocks = byWeapon.get(weaponId);
            if (stocks == null) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(stocks);
        }

        public Iterable<String> weaponIds() {
            return totals.keySet();
        }

        public Iterable<String> itemKeys() {
            return totals.keySet();
        }
    }

    public static final class MarketStockBuilder {
        private final Map<String, Integer> totals = new HashMap<String, Integer>();
        private final Map<String, List<SubmarketWeaponStock>> byWeapon = new HashMap<String, List<SubmarketWeaponStock>>();

        public void add(String weaponId, SubmarketWeaponStock stock) {
            if (weaponId == null || weaponId.isEmpty() || stock == null || stock.getCount() <= 0) {
                return;
            }
            InventoryCountService.add(totals, weaponId, stock.getCount());
            List<SubmarketWeaponStock> stocks = byWeapon.get(weaponId);
            if (stocks == null) {
                stocks = new ArrayList<SubmarketWeaponStock>();
                byWeapon.put(weaponId, stocks);
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
            return new MarketStock(totals, byWeapon);
        }
    }
}
