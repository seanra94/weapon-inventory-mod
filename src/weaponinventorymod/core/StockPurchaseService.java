package weaponinventorymod.core;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction.LineItemType;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class StockPurchaseService {
    private static final Logger LOG = Logger.getLogger(StockPurchaseService.class);

    public PurchaseResult buyCheapest(SectorAPI sector,
                                      MarketAPI market,
                                      String weaponId,
                                      int requestedQuantity,
                                      boolean includeBlackMarket) {
        return buy(sector, market, weaponId, null, requestedQuantity, includeBlackMarket);
    }

    public PurchaseResult buyFromSubmarket(SectorAPI sector,
                                           MarketAPI market,
                                           String weaponId,
                                           String submarketId,
                                           int requestedQuantity,
                                           boolean includeBlackMarket) {
        return buy(sector, market, weaponId, submarketId, requestedQuantity, includeBlackMarket);
    }

    public PurchaseResult sellToMarket(SectorAPI sector,
                                       MarketAPI market,
                                       String weaponId,
                                       int requestedQuantity,
                                       boolean includeBlackMarket) {
        if (sector == null || market == null) {
            return PurchaseResult.failure("No active market context.");
        }
        if (weaponId == null || weaponId.isEmpty()) {
            return PurchaseResult.failure("No weapon selected.");
        }
        if (requestedQuantity <= 0) {
            return PurchaseResult.failure("Nothing to sell.");
        }

        CampaignFleetAPI fleet = sector.getPlayerFleet();
        CargoAPI playerCargo = fleet == null ? null : fleet.getCargo();
        if (playerCargo == null) {
            return PurchaseResult.failure("Player cargo is unavailable.");
        }
        CargoStackAPI playerStack = playerWeaponStack(playerCargo, weaponId);
        int available = playerWeaponCount(playerCargo, weaponId);
        if (playerStack == null || available <= 0) {
            return PurchaseResult.failure("No player-cargo stock is available to sell.");
        }
        int quantity = Math.min(requestedQuantity, available);
        SellTarget target = sellTarget(market, playerStack, includeBlackMarket);
        if (target == null) {
            return PurchaseResult.failure("No valid market buyer is available.");
        }

        int credits = target.unitPrice * quantity;
        playerCargo.removeWeapons(weaponId, quantity);
        playerCargo.getCredits().add(credits);
        playerCargo.removeEmptyStacks();
        playerCargo.sort();
        playerCargo.updateSpaceUsed();
        target.cargo.addWeapons(weaponId, quantity);
        target.cargo.removeEmptyStacks();
        target.cargo.sort();
        target.cargo.updateSpaceUsed();
        reportWeaponTransaction(market, target.submarket, weaponId, quantity, target.unitPrice, false);

        String message = "Sold " + quantity + " " + weaponDisplayName(weaponId) + " for " + credits(credits) + ".";
        if (Global.getSector() != null && Global.getSector().getCampaignUI() != null) {
            Global.getSector().getCampaignUI().addMessage(message);
        }
        return PurchaseResult.success(message, quantity, -credits);
    }

    public PurchaseResult sellVirtualGlobal(SectorAPI sector,
                                            String weaponId,
                                            int requestedQuantity) {
        if (sector == null) {
            return PurchaseResult.failure("No active sector context.");
        }
        if (weaponId == null || weaponId.isEmpty()) {
            return PurchaseResult.failure("No weapon selected.");
        }
        if (requestedQuantity <= 0) {
            return PurchaseResult.failure("Nothing to sell.");
        }

        CampaignFleetAPI fleet = sector.getPlayerFleet();
        CargoAPI playerCargo = fleet == null ? null : fleet.getCargo();
        if (playerCargo == null) {
            return PurchaseResult.failure("Player cargo is unavailable.");
        }
        CargoStackAPI playerStack = playerWeaponStack(playerCargo, weaponId);
        int available = playerWeaponCount(playerCargo, weaponId);
        if (playerStack == null || available <= 0) {
            return PurchaseResult.failure("No player-cargo stock is available to sell.");
        }

        int quantity = Math.min(requestedQuantity, available);
        int credits = Math.max(0, Math.round(playerStack.getBaseValuePerUnit())) * quantity;
        playerCargo.removeWeapons(weaponId, quantity);
        playerCargo.getCredits().add(credits);
        playerCargo.removeEmptyStacks();
        playerCargo.sort();
        playerCargo.updateSpaceUsed();

        String message = "Sold " + quantity + " " + weaponDisplayName(weaponId)
                + " to the global weapon market for " + credits(credits) + ".";
        if (Global.getSector() != null && Global.getSector().getCampaignUI() != null) {
            Global.getSector().getCampaignUI().addMessage(message);
        }
        return PurchaseResult.success(message, quantity, -credits);
    }

    public PurchaseResult buyVirtualGlobal(SectorAPI sector,
                                           String weaponId,
                                           int requestedQuantity,
                                           int unitPrice,
                                           float unitCargoSpace) {
        if (sector == null) {
            return PurchaseResult.failure("No active sector context.");
        }
        if (weaponId == null || weaponId.isEmpty()) {
            return PurchaseResult.failure("No weapon selected.");
        }
        if (requestedQuantity <= 0) {
            return PurchaseResult.failure("Nothing to buy.");
        }
        if (unitPrice < 0) {
            return PurchaseResult.failure("No global-market price is available.");
        }

        CampaignFleetAPI fleet = sector.getPlayerFleet();
        CargoAPI playerCargo = fleet == null ? null : fleet.getCargo();
        if (playerCargo == null) {
            return PurchaseResult.failure("Player cargo is unavailable.");
        }
        int totalCost = unitPrice * requestedQuantity;
        float totalSpace = Math.max(1f, unitCargoSpace) * requestedQuantity;
        if (playerCargo.getCredits().get() + 0.01f < totalCost) {
            return PurchaseResult.failure("Need " + credits(totalCost) + " for this order.");
        }
        if (playerCargo.getSpaceLeft() + 0.01f < totalSpace) {
            return PurchaseResult.failure("Need " + Math.round(totalSpace) + " cargo space for this order.");
        }

        playerCargo.addWeapons(weaponId, requestedQuantity);
        playerCargo.getCredits().subtract(totalCost);
        playerCargo.removeEmptyStacks();
        playerCargo.sort();
        playerCargo.updateSpaceUsed();

        String message = "Bought " + requestedQuantity + " " + weaponDisplayName(weaponId)
                + " from the global weapon market for " + credits(totalCost) + ".";
        if (Global.getSector() != null && Global.getSector().getCampaignUI() != null) {
            Global.getSector().getCampaignUI().addMessage(message);
        }
        return PurchaseResult.success(message, requestedQuantity, totalCost);
    }

    private PurchaseResult buy(SectorAPI sector,
                               MarketAPI market,
                               String weaponId,
                               String onlySubmarketId,
                               int requestedQuantity,
                               boolean includeBlackMarket) {
        if (sector == null || market == null) {
            return PurchaseResult.failure("No active market context.");
        }
        if (weaponId == null || weaponId.isEmpty()) {
            return PurchaseResult.failure("No weapon selected.");
        }
        if (requestedQuantity <= 0) {
            return PurchaseResult.failure("Nothing to buy.");
        }

        CampaignFleetAPI fleet = sector.getPlayerFleet();
        CargoAPI playerCargo = fleet == null ? null : fleet.getCargo();
        if (playerCargo == null) {
            return PurchaseResult.failure("Player cargo is unavailable.");
        }

        List<PurchaseSource> sources = collectSources(market, weaponId, onlySubmarketId, includeBlackMarket);
        if (sources.isEmpty()) {
            return PurchaseResult.failure("No purchasable stock is available.");
        }
        Collections.sort(sources, PurchaseSource.PRICE_ORDER);

        int remaining = requestedQuantity;
        int totalQuantity = 0;
        int totalCost = 0;
        float totalSpace = 0f;
        List<PurchaseLine> plan = new ArrayList<PurchaseLine>();
        for (PurchaseSource source : sources) {
            if (remaining <= 0) {
                break;
            }
            int quantity = Math.min(remaining, source.available);
            if (quantity <= 0) {
                continue;
            }
            plan.add(new PurchaseLine(source, quantity));
            remaining -= quantity;
            totalQuantity += quantity;
            totalCost += source.unitPrice * quantity;
            totalSpace += source.unitCargoSpace * quantity;
        }

        if (totalQuantity <= 0) {
            return PurchaseResult.failure("No purchasable stock is available.");
        }
        if (playerCargo.getCredits().get() + 0.01f < totalCost) {
            return PurchaseResult.failure("Need " + credits(totalCost) + " for this order.");
        }
        if (playerCargo.getSpaceLeft() + 0.01f < totalSpace) {
            return PurchaseResult.failure("Need " + Math.round(totalSpace) + " cargo space for this order.");
        }

        for (PurchaseLine line : plan) {
            line.source.cargo.removeWeapons(weaponId, line.quantity);
            line.source.cargo.removeEmptyStacks();
            line.source.cargo.sort();
            line.source.cargo.updateSpaceUsed();
            reportWeaponTransaction(market, line.source.submarket, weaponId, line.quantity, line.source.unitPrice, true);
        }
        playerCargo.addWeapons(weaponId, totalQuantity);
        playerCargo.getCredits().subtract(totalCost);
        playerCargo.removeEmptyStacks();
        playerCargo.sort();
        playerCargo.updateSpaceUsed();

        String message = "Bought " + totalQuantity + " " + weaponDisplayName(weaponId) + " for " + credits(totalCost) + ".";
        if (Global.getSector() != null && Global.getSector().getCampaignUI() != null) {
            Global.getSector().getCampaignUI().addMessage(message);
        }
        return PurchaseResult.success(message, totalQuantity, totalCost);
    }

    private static List<PurchaseSource> collectSources(MarketAPI market,
                                                       String weaponId,
                                                       String onlySubmarketId,
                                                       boolean includeBlackMarket) {
        List<PurchaseSource> result = new ArrayList<PurchaseSource>();
        if (market.getSubmarketsCopy() == null) {
            return result;
        }
        for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
            if (onlySubmarketId != null && !onlySubmarketId.equals(submarket.getSpecId())) {
                continue;
            }
            if (Submarkets.SUBMARKET_STORAGE.equals(submarket.getSpecId()) || Submarkets.LOCAL_RESOURCES.equals(submarket.getSpecId())) {
                continue;
            }
            if (!includeBlackMarket && Submarkets.SUBMARKET_BLACK.equals(submarket.getSpecId())) {
                continue;
            }
            CargoAPI cargo = submarket.getCargoNullOk();
            if (cargo == null || cargo.getStacksCopy() == null) {
                continue;
            }
            for (CargoStackAPI stack : cargo.getStacksCopy()) {
                if (!MarketStockService.isPurchasableWeaponStack(submarket, stack)) {
                    continue;
                }
                if (!weaponId.equals(stack.getWeaponSpecIfWeapon().getWeaponId())) {
                    continue;
                }
                int available = Math.round(stack.getSize());
                if (available > 0) {
                    result.add(new PurchaseSource(submarket, cargo, available,
                            MarketStockService.unitPrice(submarket, stack),
                            MarketStockService.unitCargoSpace(stack)));
                }
            }
        }
        return result;
    }

    private static SellTarget sellTarget(MarketAPI market, CargoStackAPI playerStack, boolean includeBlackMarket) {
        if (market == null || market.getSubmarketsCopy() == null || playerStack == null) {
            return null;
        }
        SellTarget best = null;
        for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
            if (submarket == null) {
                continue;
            }
            if (Submarkets.SUBMARKET_STORAGE.equals(submarket.getSpecId()) || Submarkets.LOCAL_RESOURCES.equals(submarket.getSpecId())) {
                continue;
            }
            if (!includeBlackMarket && Submarkets.SUBMARKET_BLACK.equals(submarket.getSpecId())) {
                continue;
            }
            CargoAPI cargo = submarket.getCargoNullOk();
            if (cargo == null) {
                continue;
            }
            SubmarketPlugin plugin = submarket.getPlugin();
            if (plugin != null && plugin.isIllegalOnSubmarket(playerStack, SubmarketPlugin.TransferAction.PLAYER_SELL)) {
                continue;
            }
            SellTarget candidate = new SellTarget(submarket, cargo, MarketStockService.sellUnitPrice(submarket, playerStack));
            if (best == null || candidate.unitPrice > best.unitPrice) {
                best = candidate;
            }
        }
        return best;
    }

    private static CargoStackAPI playerWeaponStack(CargoAPI playerCargo, String weaponId) {
        if (playerCargo == null || playerCargo.getStacksCopy() == null) {
            return null;
        }
        for (CargoStackAPI stack : playerCargo.getStacksCopy()) {
            if (MarketStockService.isVisibleWeaponStack(stack) && weaponId.equals(stack.getWeaponSpecIfWeapon().getWeaponId())) {
                return stack;
            }
        }
        return null;
    }

    private static int playerWeaponCount(CargoAPI playerCargo, String weaponId) {
        int count = 0;
        if (playerCargo == null || playerCargo.getStacksCopy() == null) {
            return count;
        }
        for (CargoStackAPI stack : playerCargo.getStacksCopy()) {
            if (MarketStockService.isVisibleWeaponStack(stack) && weaponId.equals(stack.getWeaponSpecIfWeapon().getWeaponId())) {
                count += Math.round(stack.getSize());
            }
        }
        return count;
    }

    private static String weaponDisplayName(String weaponId) {
        try {
            return Global.getSettings().getWeaponSpec(weaponId).getWeaponName();
        } catch (Throwable ignored) {
            return weaponId;
        }
    }

    private static void reportWeaponTransaction(MarketAPI market,
                                                SubmarketAPI submarket,
                                                String weaponId,
                                                int quantity,
                                                int unitPrice,
                                                boolean bought) {
        if (market == null || submarket == null || weaponId == null || weaponId.isEmpty() || quantity <= 0) {
            return;
        }
        SubmarketPlugin plugin = submarket.getPlugin();
        if (plugin == null) {
            return;
        }
        try {
            PlayerMarketTransaction transaction = new PlayerMarketTransaction(market, submarket, tradeMode(submarket));
            CargoAPI cargo = Global.getFactory() == null ? null : Global.getFactory().createCargo(false);
            if (cargo != null) {
                cargo.addWeapons(weaponId, quantity);
                cargo.sort();
                if (bought) {
                    transaction.setBought(cargo);
                } else {
                    transaction.setSold(cargo);
                }
            }
            LineItemType lineType = bought ? LineItemType.BOUGHT : LineItemType.SOLD;
            transaction.getLineItems().add(new PlayerMarketTransaction.TransactionLineItem(
                    weaponId,
                    lineType,
                    CargoItemType.WEAPONS,
                    submarket,
                    quantity,
                    unitPrice,
                    unitPrice,
                    timestamp()));
            transaction.setCreditValue(bought ? unitPrice * quantity : -unitPrice * quantity);
            plugin.reportPlayerMarketTransaction(transaction);
        } catch (Throwable t) {
            // Transaction callbacks are best-effort; cargo mutation has already succeeded.
            LOG.warn("WIM_STOCK_REVIEW transaction report failed for " + weaponId
                    + " at " + submarket.getSpecId(), t);
        }
    }

    private static CampaignUIAPI.CoreUITradeMode tradeMode(SubmarketAPI submarket) {
        SubmarketPlugin plugin = submarket == null ? null : submarket.getPlugin();
        return plugin != null && plugin.isBlackMarket()
                ? CampaignUIAPI.CoreUITradeMode.SNEAK
                : CampaignUIAPI.CoreUITradeMode.OPEN;
    }

    private static long timestamp() {
        return Global.getSector() == null || Global.getSector().getClock() == null
                ? 0L
                : Global.getSector().getClock().getTimestamp();
    }

    private static String credits(int credits) {
        String digits = String.valueOf(Math.abs(credits));
        StringBuilder result = new StringBuilder();
        int firstGroup = digits.length() % 3;
        if (firstGroup == 0) {
            firstGroup = 3;
        }
        result.append(digits.substring(0, firstGroup));
        for (int i = firstGroup; i < digits.length(); i += 3) {
            result.append(',').append(digits.substring(i, i + 3));
        }
        return (credits < 0 ? "-" : "") + result.toString() + " credits";
    }

    private static final class PurchaseSource {
        static final Comparator<PurchaseSource> PRICE_ORDER = new Comparator<PurchaseSource>() {
            @Override
            public int compare(PurchaseSource left, PurchaseSource right) {
                int result = Integer.compare(left.unitPrice, right.unitPrice);
                if (result != 0) {
                    return result;
                }
                return left.submarket.getNameOneLine().compareToIgnoreCase(right.submarket.getNameOneLine());
            }
        };

        final SubmarketAPI submarket;
        final CargoAPI cargo;
        final int available;
        final int unitPrice;
        final float unitCargoSpace;

        PurchaseSource(SubmarketAPI submarket, CargoAPI cargo, int available, int unitPrice, float unitCargoSpace) {
            this.submarket = submarket;
            this.cargo = cargo;
            this.available = available;
            this.unitPrice = unitPrice;
            this.unitCargoSpace = unitCargoSpace;
        }
    }

    private static final class PurchaseLine {
        final PurchaseSource source;
        final int quantity;

        PurchaseLine(PurchaseSource source, int quantity) {
            this.source = source;
            this.quantity = quantity;
        }
    }

    private static final class SellTarget {
        final SubmarketAPI submarket;
        final CargoAPI cargo;
        final int unitPrice;

        SellTarget(SubmarketAPI submarket, CargoAPI cargo, int unitPrice) {
            this.submarket = submarket;
            this.cargo = cargo;
            this.unitPrice = unitPrice;
        }
    }

    public static final class PurchaseResult {
        private final boolean success;
        private final String message;
        private final int quantity;
        private final int credits;

        private PurchaseResult(boolean success, String message, int quantity, int credits) {
            this.success = success;
            this.message = message;
            this.quantity = quantity;
            this.credits = credits;
        }

        public static PurchaseResult success(String message, int quantity, int credits) {
            return new PurchaseResult(true, message, quantity, credits);
        }

        public static PurchaseResult failure(String message) {
            return new PurchaseResult(false, message, 0, 0);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public int getQuantity() {
            return quantity;
        }

        public int getCredits() {
            return credits;
        }
    }
}
