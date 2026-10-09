package tankops.common.model.environment;

import tankops.common.model.common.Vector2D;

public class Ice extends Environment {

    private static final double SLIDE_DISTANCE = 16.0;

    public Ice(String id, Vector2D position) {
        super(id, position);
    }

    @Override public boolean canTankPass() { return true; }
    @Override public boolean canBulletPass() { return true; }
    @Override public void takeHit(int starLevel) {}
    @Override public boolean isDestroyed() { return false; }


    public boolean isSlippery() {
        return true;
    }


    public double getSlideDistance() {
        return SLIDE_DISTANCE;
    }
}
