package weaponinventorymod.core;

public final class SubmarketWeaponStock {
    private final String submarketId;
    private final String submarketName;
    private final int count;
    private final int unitPrice;
    private final float unitCargoSpace;

    public SubmarketWeaponStock(String submarketId, String submarketName, int count, int unitPrice, float unitCargoSpace) {
        this.submarketId = submarketId;
        this.submarketName = submarketName;
        this.count = count;
        this.unitPrice = unitPrice;
        this.unitCargoSpace = unitCargoSpace;
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
}
