package tankops.common.model;

public enum EnvironmentType {
    //             tankPassable, bulletPassable, destructible, durability, normalDmg, maxTierDmg
    BRICK_WALL(false, false, true,  4, 1, 2),
    STEEL_WALL(false, false, true,  2, 0, 1),
    TREES     (true,  true,  false, 0, 0, 0),
    WATER     (false, true,  false, 0, 0, 0),
    ICE       (true,  true,  false, 0, 0, 0);

    public static final int MAX_STAR_LEVEL = 3; // star 3 = Tier 4

    private final boolean tankPassable;
    private final boolean bulletPassable;
    private final boolean destructible;
    private final int durability;
    private final int normalDamage;
    private final int maxTierDamage;

    EnvironmentType(boolean tankPassable, boolean bulletPassable, boolean destructible,
                    int durability, int normalDamage, int maxTierDamage) {
        this.tankPassable = tankPassable;
        this.bulletPassable = bulletPassable;
        this.destructible = destructible;
        this.durability = durability;
        this.normalDamage = normalDamage;
        this.maxTierDamage = maxTierDamage;
    }

    public boolean isTankPassable()   { return tankPassable; }
    public boolean isBulletPassable() { return bulletPassable; }
    public boolean isDestructible()   { return destructible; }
    public int getDefaultDurability() { return durability; }

    public int damageFrom(int bulletStarLevel) {
        if (!destructible) return 0;
        return bulletStarLevel >= MAX_STAR_LEVEL ? maxTierDamage : normalDamage;
    }
}