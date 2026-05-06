package weaponinventorymod.core;

public final class SubmarketWeaponStock {
    private final String submarketId;
    private final String submarketName;
    private final int count;
    private final int unitPrice;
    private final float unitCargoSpace;
    private final boolean purchasable;

    public SubmarketWeaponStock(String submarketId,
                                String submarketName,
                                int count,
                                int unitPrice,
                                float unitCargoSpace,
                                boolean purchasable) {
        this.submarketId = submarketId;
        this.submarketName = submarketName;
        this.count = count;
        this.unitPrice = unitPrice;
        this.unitCargoSpace = unitCargoSpace;
        this.purchasable = purchasable;
    }

    public String getSubmarketId() {
        return submarketId;
    }

    public String getSubmarketName() {
        return submarketName;
    }

    public int getCount() {
        return count;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public float getUnitCargoSpace() {
        return unitCargoSpace;
    }

    public boolean isPurchasable() {
        return purchasable;
    }
}
