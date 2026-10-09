package tankops.common.model.tank;

import tankops.common.model.common.Bullet;
import tankops.common.model.common.Direction;
import tankops.common.model.common.Vector2D;

public class ArmorTank extends EnemyTank {
    public ArmorTank(String id, Vector2D position, Direction direction) {
        super(id, position, direction, MOVE_NORMAL, 4, 400);
    }

    @Override public double getBulletSpeed() { return Bullet.SPEED_NORMAL; }
}
