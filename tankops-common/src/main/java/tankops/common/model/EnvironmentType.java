package tankops.common.model;

public enum EnvironmentType {
    BRICK_WALL(false, false, true, 4),   // [canTankPass, canBulletPass, isDestructible, defaultDurability]
    STEEL_WALL(false, false, true, 2),   // Chỉ bị phá bởi đạn Tier 4
    TREES(true, true, false, 0),         // Che phủ xe tăng & đạn
    WATER(false, true, false, 0),        // Xe không qua được, đạn bay qua được
    ICE(true, true, false, 0);           // Xe đi qua bị trượt

    private final boolean canTankPass;
    private final boolean canBulletPass;
    private final boolean isDestructible;
    private final int defaultDurability;

    EnvironmentType(boolean canTankPass, boolean canBulletPass, boolean isDestructible, int defaultDurability) {
        this.canTankPass = canTankPass;
        this.canBulletPass = canBulletPass;
        this.isDestructible = isDestructible;
        this.defaultDurability = defaultDurability;
    }

    public boolean isCanTankPass() { return canTankPass; }
    public boolean isCanBulletPass() { return canBulletPass; }
    public boolean isDestructible() { return isDestructible; }
    public int getDefaultDurability() { return defaultDurability; }
}