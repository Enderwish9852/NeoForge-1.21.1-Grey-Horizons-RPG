package net.enderwish.Belliarium_Monstrarium_Subpack.core;

public enum ItemRarity {
    COMMON(1, 0xFFAAAAAA),
    UNCOMMON(2, 0xFF5599FF),
    RARE(3, 0xFFAA55FF),
    LEGENDARY(4, 0xFFFFAA00);

    private final int maxRepairCharges;
    private final int color;

    ItemRarity(int maxRepairCharges, int color) {
        this.maxRepairCharges = maxRepairCharges;
        this.color = color;
    }

    public int getMaxRepairCharges() { return maxRepairCharges; }
    public int getColor() { return color; }
}
