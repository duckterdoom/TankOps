package tankops.common.model.tank;


import tankops.common.model.common.Direction;
import tankops.common.model.common.Vector2D;

public abstract class EnemyTank extends Tank {
    private final int points;
    private boolean frozen = false;

    public EnemyTank(String id, Vector2D position, Direction direction, double moveSpeed, int maxHealth, int points) {
        super(id, position, direction, moveSpeed, maxHealth);
        this.points = points;
    }

    public int getPoints() { return points; }
    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }

    @Override
    public int getBulletStarLevel() { return 0; }
}

