package tankops.common.model;

public class Bullet extends Entity {
    private final String ownerId;
    private final Direction direction;
    private final double speed;
    private final int damage;
    private final int starLevel;

    public Bullet(String id, String ownerId, Vector2D position, Direction direction, double speed, int damage, int starLevel) {
        super(id, position, 8.0, 8.0); // Kích thước viên đạn 8x8px
        this.ownerId = ownerId;
        this.direction = direction;
        this.speed = speed;
        this.damage = damage;
        this.starLevel = starLevel;
    }

    public String getOwnerId() { return ownerId; }
    public Direction getDirection() { return direction; }
    public double getSpeed() { return speed; }
    public int getDamage() { return damage; }
    public int getStarLevel() { return starLevel; }
}