package tankops.common.model.tank;


import tankops.common.model.common.Bullet;
import tankops.common.model.common.Direction;
import tankops.common.model.common.Vector2D;

public class BasicTank extends EnemyTank {
    public BasicTank(String id, Vector2D position, Direction direction) {
        super(id, position, direction, MOVE_SLOW, 1, 100);
    }

    @Override public double getBulletSpeed() { return Bullet.SPEED_SLOW; }
}
