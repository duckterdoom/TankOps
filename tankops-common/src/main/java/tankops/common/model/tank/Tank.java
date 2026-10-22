package tankops.common.model.tank;


import tankops.common.model.common.Bullet;
import tankops.common.model.common.Direction;
import tankops.common.model.common.Entity;
import tankops.common.model.common.Vector2D;


public abstract class Tank extends Entity {
    public static final double TANK_SIZE = 28.0;
    public static final double MOVE_SLOW = 1.0;
    public static final double MOVE_NORMAL = 2.0;
    public static final double MOVE_FAST = 3.0;

    private Direction direction;
    private final double moveSpeed;
    private int health;
    private final int maxHealth;

    private boolean isMoving = false;

    private double slideDistanceRemaining = 0.0;

    private long lastShootTime = 0; // Thời điểm bắn gần nhất (ms)
    private long shootCooldownMillis = 500;
    public Tank(String id, Vector2D position, Direction direction, double moveSpeed, int maxHealth) {
        super(id, position, TANK_SIZE, TANK_SIZE);
        this.direction = direction;
        this.moveSpeed = moveSpeed;
        this.health = maxHealth;
        this.maxHealth = maxHealth;
    }

    public Direction getDirection() { return direction; }
    public void setDirection(Direction direction) { this.direction = direction; }
    public double getMoveSpeed() { return moveSpeed; }
    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
    public int getMaxHealth() { return maxHealth; }
    public boolean isMoving(){return isMoving;}
    public void setMoving(boolean moving) { this.isMoving = moving; }

    public void takeDamage(int amount) {
        this.health = Math.max(0, this.health - amount);
    }

    public boolean isDestroyed() { return this.health <= 0; }

    public abstract double getBulletSpeed();
    public abstract int getBulletStarLevel();

    public boolean canShoot() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastShootTime) >= shootCooldownMillis;
    }
    public Bullet fire(String bulletId) {
        if (!canShoot()) {
            return null;
        }
        this.lastShootTime = System.currentTimeMillis();
        double centerX = getPosition().x() + TANK_SIZE / 2.0;
        double centerY = getPosition().y() + TANK_SIZE / 2.0;
        double offset = 16.0;
        Vector2D spawnPos = new Vector2D(
                centerX + direction.getDx() * offset - (Bullet.SIZE / 2.0),
                centerY  + direction.getDy() * offset - (Bullet.SIZE / 2.0)
        );
        return new Bullet(bulletId, getId(), spawnPos, direction, getBulletSpeed(), 1, getBulletStarLevel());
    }

    public void move(Direction newDirection){
        this.direction = newDirection;
        Vector2D newPos = getPosition().add(newDirection.toVector().multiply(moveSpeed)) ;
        setPosition(newPos);

    }

    public boolean isSliding() {
        return slideDistanceRemaining > 0;
    }

    public void triggerSlide(double distance) {
        this.slideDistanceRemaining = distance;
    }
    public void slideForward(double deltaTime) {
        if (!isSliding()) return;

        double moveStep = getMoveSpeed() * 60.0 * deltaTime; // Tính khoảng di chuyển trong frame
        double actualStep = Math.min(moveStep, slideDistanceRemaining);

        // Cập nhật vị trí mới theo hướng hiện tại
        double newX = getPosition().x() + direction.getDx() * actualStep;
        double newY = getPosition().y() + direction.getDy() * actualStep;
        setPosition(new Vector2D(newX, newY));

        // Trừ bớt quãng đường trượt còn lại
        slideDistanceRemaining -= actualStep;
    }

    public long getShootCooldownMillis() {
        return shootCooldownMillis;
    }

    public void setShootCooldownMillis(long shootCooldownMillis) {
        this.shootCooldownMillis = shootCooldownMillis;
    }
}