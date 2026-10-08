package tankops.common.model;

import java.util.Objects;

public abstract class Tank extends Entity {
    public static final double SIZE = Environment.TILE_SIZE;

    // 3 mức tốc độ di chuyển theo đề: Slow / Normal / Fast
    public static final double MOVE_SLOW = 80.0;
    public static final double MOVE_NORMAL = 120.0;
    public static final double MOVE_FAST = 160.0;

    private final double speed;
    private final int maxHp;
    private int hp;
    private Direction direction;
    private double shieldTimeLeft; // giây; > 0 nghĩa là đang có khiên (Helmet)

    protected Tank(String id, Vector2D position, Direction direction, double speed, int maxHp) {
        super(id, position, SIZE, SIZE);
        this.direction = Objects.requireNonNull(direction, "direction");
        this.speed = speed;
        this.maxHp = maxHp;
        this.hp = maxHp;
    }


    public abstract double getBulletSpeed();


    public int getMaxActiveBullets() { return 1; }


    public int getBulletStarLevel()  { return 0; }


    public int getBulletDamage()     { return 1; }

    public Direction getDirection() { return direction; }
    public double getSpeed()        { return speed; }
    public int getHp()              { return hp; }
    public int getMaxHp()           { return maxHp; }
    public boolean isAlive()        { return hp > 0; }

    public void setDirection(Direction direction) {
        this.direction = Objects.requireNonNull(direction, "direction");
    }

    public void takeDamage(int damage) {
        if (!isAlive() || isShielded() || damage <= 0) return;
        hp = Math.max(0, hp - damage);
    }

    public void respawn(Vector2D spawnPoint) {
        hp = maxHp;
        setPosition(spawnPoint);
    }

    // ---- Khiên (Helmet, và khiên đầu mỗi màn) ----
    public boolean isShielded() { return shieldTimeLeft > 0; }

    public void activateShield(double seconds) {
        shieldTimeLeft = Math.max(shieldTimeLeft, seconds);
    }

    public void updateShield(double deltaSeconds) {
        shieldTimeLeft = Math.max(0, shieldTimeLeft - deltaSeconds);
    }
}
