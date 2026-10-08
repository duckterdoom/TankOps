package tankops.common.model.environment;

import tankops.common.model.common.Vector2D;

public class Water extends Environment {
    public Water(String id, Vector2D position) { super(id, position); }
    @Override public boolean canTankPass() { return false; }
    @Override public boolean canBulletPass() { return true; }
    @Override public void takeHit(int starLevel) {}
    @Override public boolean isDestroyed() { return false; }
}
