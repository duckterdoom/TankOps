package tankops.common.model;

public class EnemyTank extends Tank {
    private final EnemyType type;
    private final int scoreValue;
    private boolean isFrozen = false;

    public EnemyTank(String id, Vector2D position, Direction direction, EnemyType type) {
        super(id, position, direction, type.getSpeed(), type.getBulletSpeed(), type.getMaxHp());
        this.type = type;
        this.scoreValue = type.getScoreValue();
    }

    public EnemyType getType() { return type; }
    public int getScoreValue() { return scoreValue; }
    public boolean isFrozen() { return isFrozen; }
    public void setFrozen(boolean frozen) { this.isFrozen = frozen; }
}