package tankops.common.model;

import java.util.Objects;

public class EnemyTank extends Tank {
    private final EnemyType type;
    private boolean frozen = false;

    public EnemyTank(String id, Vector2D position, Direction direction, EnemyType type) {
        super(id, position, direction,
                Objects.requireNonNull(type, "type").getSpeed(), type.getMaxHp());
        this.type = type;
    }

    public EnemyType getType()  { return type; }
    public int getScoreValue()  { return type.getScoreValue(); }
    public boolean isFrozen()   { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }

    @Override public double getBulletSpeed() { return type.getBulletSpeed(); }
}