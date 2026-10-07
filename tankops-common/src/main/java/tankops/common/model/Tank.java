package tankops.common.model;

public abstract class Tank extends Entity {
    protected Direction direction;
    protected double speed;
    protected double bulletSpeed;
    protected int hp;
    protected int maxHp;
    protected boolean alive;

    public Tank(String id, Vector2D position, Direction direction, double speed, double bulletSpeed, int hp) {
        super(id, position, 32.0, 32.0); // Kích thước xe tăng chuẩn 32x32px
        this.direction = direction;
        this.speed = speed;
        this.bulletSpeed = bulletSpeed;
        this.hp = hp;
        this.maxHp = hp;
        this.alive = true;
    }

    public Direction getDirection() { return direction; }
    public void setDirection(Direction direction) { this.direction = direction; }
    public double getSpeed() { return speed; }
    public double getBulletSpeed() { return bulletSpeed; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public boolean isAlive() { return alive; }

    public void takeDamage(int damage) {
        if (!alive) return;
        this.hp -= damage;
        if (this.hp <= 0) {
            this.hp = 0;
            this.alive = false;
        }
    }
}