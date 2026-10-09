package tankops.common.model.environment;

import tankops.common.model.common.Vector2D;

public class SteelWall extends Environment {
    private int durability = 2;

    public SteelWall(String id, Vector2D position) { super(id, position); }

    @Override public boolean canTankPass() { return false; }
    @Override public boolean canBulletPass() { return false; }

    @Override
    public void takeHit(int starLevel) {
        if (isDestroyed()) return;
        if (starLevel >= 3) {
            durability = Math.max(0, durability - 1);
        }
    }

    @Override public boolean isDestroyed() { return durability <= 0; }
    public int getDurability() { return durability; }
}
