package weaponinventorymod.core;

public final class SubmarketWeaponStock {
    private final String marketId;
    private final String marketName;
    private final String submarketId;
    private final String submarketName;
    private final int count;
    private final int unitPrice;
    private final int baseUnitPrice;
    private final float unitCargoSpace;
    private final boolean purchasable;

    public SubmarketWeaponStock(String submarketId,
                                String submarketName,
                                int count,
                                int unitPrice,
                                int baseUnitPrice,
                                float unitCargoSpace,
                                boolean purchasable) {
        this(null, null, submarketId, submarketName, count, unitPrice, baseUnitPrice, unitCargoSpace, purchasable);
    }

    public SubmarketWeaponStock(String submarketId,
                                String submarketName,
                                int count,
                                int unitPrice,
                                float unitCargoSpace,
                                boolean purchasable) {
        this(null, null, submarketId, submarketName, count, unitPrice, unitPrice, unitCargoSpace, purchasable);
    }

    public SubmarketWeaponStock(String marketId,
                                String marketName,
                                String submarketId,
                                String submarketName,
                                int count,
                                int unitPrice,
                                float unitCargoSpace,
                                boolean purchasable) {
        this(marketId, marketName, submarketId, submarketName, count, unitPrice, unitPrice, unitCargoSpace, purchasable);
    }

    public SubmarketWeaponStock(String marketId,
                                String marketName,
                                String submarketId,
                                String submarketName,
                                int count,
                                int unitPrice,
                                int baseUnitPrice,
                                float unitCargoSpace,
                                boolean purchasable) {
        this.marketId = marketId;
        this.marketName = marketName;
        this.submarketId = submarketId;
        this.submarketName = submarketName;
        this.count = count;
        this.unitPrice = unitPrice;
        this.baseUnitPrice = baseUnitPrice;
        this.unitCargoSpace = unitCargoSpace;
        this.purchasable = purchasable;
    }

    public String getMarketId() {
        return marketId;
    }

    public String getMarketName() {
        return marketName;
    }

    public String getSubmarketId() {
        return submarketId;
    }

    public String getSubmarketName() {
        return submarketName;
    }

    public String getSourceId() {
        if (marketId == null || marketId.isEmpty()) {
            return submarketId;
        }
        return marketId + "|" + submarketId;
    }

    public String getDisplaySourceName() {
        if (marketName == null || marketName.isEmpty()) {
            return submarketName;
        }
        return marketName + " / " + submarketName;
    }

    public int getCount() {
        return count;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public int getBaseUnitPrice() {
        return baseUnitPrice;
    }

    public float getUnitCargoSpace() {
        return unitCargoSpace;
    }

    public boolean isPurchasable() {
        return purchasable;
    }
}
