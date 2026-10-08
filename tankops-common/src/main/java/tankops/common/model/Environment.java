package tankops.common.model;

public class Environment extends Entity {
    public static final double TILE_SIZE = 32.0;

    private final EnvironmentType type;
    private int durability;

    public Environment(String id, Vector2D position, EnvironmentType type) {
        super(id, position, TILE_SIZE, TILE_SIZE);
        this.type = type;
        this.durability = type.getDefaultDurability();
    }

    public EnvironmentType getType() { return type; }
    public int getDurability()       { return durability; }

    public boolean canTankPass()   { return type.isTankPassable(); }
    public boolean canBulletPass() { return type.isBulletPassable(); }

    public void takeHit(int bulletStarLevel) {
        if (isDestroyed()) return;
        durability -= type.damageFrom(bulletStarLevel);
        if (durability < 0) durability = 0;
    }

    public boolean isDestroyed() {
        return type.isDestructible() && durability <= 0;
    }
}