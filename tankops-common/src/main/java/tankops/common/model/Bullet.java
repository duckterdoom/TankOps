package tankops.common.model;

import java.util.Objects;

public class Bullet extends Entity {
    //Star tối đa (star 3 = tier 4).
    public static final int MAX_STAR_LEVEL = 3;

    // 3 mức tốc độ đạn theo đề: Slow / Normal / Fast
    public static final double SPEED_SLOW = 180.0;
    public static final double SPEED_NORMAL = 240.0;
    public static final double SPEED_FAST = 300.0;

    public static final double SIZE = 8.0;

    private final String ownerId;
    private final Direction direction;
    private final double speed;
    private final int damage;
    private final int starLevel; // chốt lúc bắn, không đổi khi đạn đang bay

    public Bullet(String id, String ownerId, Vector2D position, Direction direction,
                  double speed, int damage, int starLevel) {
        super(id, position, SIZE, SIZE);
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.direction = Objects.requireNonNull(direction, "direction");
        this.speed = speed;
        this.damage = damage;
        this.starLevel = Math.clamp(starLevel, 0, MAX_STAR_LEVEL);
    }

    public String getOwnerId()      { return ownerId; }
    public Direction getDirection() { return direction; }
    public double getSpeed()        { return speed; }
    public int getDamage()          { return damage; }
    public int getStarLevel()       { return starLevel; }
}
