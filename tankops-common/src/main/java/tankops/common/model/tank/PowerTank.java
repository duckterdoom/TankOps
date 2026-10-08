package tankops.common.model.tank;

import tankops.common.model.common.Bullet;
import tankops.common.model.common.Direction;
import tankops.common.model.common.Vector2D;

public class PowerTank extends EnemyTank {
    public PowerTank(String id, Vector2D position, Direction direction) {
        super(id, position, direction, MOVE_NORMAL, 1, 300);
    }

    @Override public double getBulletSpeed() { return Bullet.SPEED_FAST; }
}
