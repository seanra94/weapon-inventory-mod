package weaponinventorymod.core;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class StockPurchaseService {
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
            return PurchaseResult.failure("Need " + totalCost + " credits for this order.");
        }
        if (playerCargo.getSpaceLeft() + 0.01f < totalSpace) {
            return PurchaseResult.failure("Need " + Math.round(totalSpace) + " cargo space for this order.");
        }

        for (PurchaseLine line : plan) {
            line.source.cargo.removeWeapons(weaponId, line.quantity);
            line.source.cargo.removeEmptyStacks();
            line.source.cargo.updateSpaceUsed();
            line.source.cargo.getCredits().add(line.source.unitPrice * line.quantity);
        }
        playerCargo.addWeapons(weaponId, totalQuantity);
        playerCargo.getCredits().subtract(totalCost);
        playerCargo.updateSpaceUsed();

        String message = "Bought " + totalQuantity + " " + weaponDisplayName(weaponId) + " for " + totalCost + " credits.";
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

    private static String weaponDisplayName(String weaponId) {
        try {
            return Global.getSettings().getWeaponSpec(weaponId).getWeaponName();
        } catch (Throwable ignored) {
            return weaponId;
        }
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
