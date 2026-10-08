package tankops.common.model.environment;

import tankops.common.model.common.Vector2D;

public class BrickWall extends Environment {
    private int durability = 4;

    public BrickWall(String id, Vector2D position) { super(id, position); }

    @Override public boolean canTankPass() { return false; }
    @Override public boolean canBulletPass() { return false; }

    @Override
    public void takeHit(int starLevel) {
        if (isDestroyed()) return;
        int damage = (starLevel >= 3) ? 2 : 1;
        durability = Math.max(0, durability - damage);
    }

    @Override public boolean isDestroyed() { return durability <= 0; }
    public int getDurability() { return durability; }
}
