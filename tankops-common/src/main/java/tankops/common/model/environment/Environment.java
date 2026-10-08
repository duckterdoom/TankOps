package tankops.common.model.environment;


import tankops.common.model.common.Entity;
import tankops.common.model.common.Vector2D;

public abstract class Environment extends Entity {
    public static final double TILE_SIZE = 32.0;

    public Environment(String id, Vector2D position) {
        super(id, position, TILE_SIZE, TILE_SIZE);
    }

    public abstract boolean canTankPass();
    public abstract boolean canBulletPass();
    public abstract void takeHit(int starLevel);
    public abstract boolean isDestroyed();
}


