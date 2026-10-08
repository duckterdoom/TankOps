package tankops.common.model.tank;

import tankops.common.model.common.Bullet;
import tankops.common.model.common.Direction;
import tankops.common.model.common.Vector2D;

public class FastTank extends EnemyTank {
    public FastTank(String id, Vector2D position, Direction direction) {
        super(id, position, direction, MOVE_FAST, 1, 200);
    }

    @Override public double getBulletSpeed() { return Bullet.SPEED_NORMAL; }
}
