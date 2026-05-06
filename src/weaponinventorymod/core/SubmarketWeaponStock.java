package weaponinventorymod.core;

public final class SubmarketWeaponStock {
    private final String submarketId;
    private final String submarketName;
    private final int count;

    public SubmarketWeaponStock(String submarketId, String submarketName, int count) {
        this.submarketId = submarketId;
        this.submarketName = submarketName;
        this.count = count;
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
}
