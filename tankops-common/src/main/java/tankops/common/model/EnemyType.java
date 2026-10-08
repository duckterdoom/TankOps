package tankops.common.model;

public enum EnemyType {
    //    điểm, hp, tốc độ di chuyển,   tốc độ đạn
    BASIC(100, 1, Tank.MOVE_SLOW,   Bullet.SPEED_SLOW),
    FAST (200, 1, Tank.MOVE_FAST,   Bullet.SPEED_NORMAL),
    POWER(300, 1, Tank.MOVE_NORMAL, Bullet.SPEED_FAST),
    ARMOR(400, 4, Tank.MOVE_NORMAL, Bullet.SPEED_NORMAL);

    private final int scoreValue;
    private final int maxHp;
    private final double speed;
    private final double bulletSpeed;

    EnemyType(int scoreValue, int maxHp, double speed, double bulletSpeed) {
        this.scoreValue = scoreValue;
        this.maxHp = maxHp;
        this.speed = speed;
        this.bulletSpeed = bulletSpeed;
    }

    public int getScoreValue()     { return scoreValue; }
    public int getMaxHp()          { return maxHp; }
    public double getSpeed()       { return speed; }
    public double getBulletSpeed() { return bulletSpeed; }
}
