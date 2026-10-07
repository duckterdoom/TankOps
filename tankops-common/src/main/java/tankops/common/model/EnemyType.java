package tankops.common.model;

public enum EnemyType {
    BASIC(100, 1, 80.0, 180.0),
    FAST(200, 1, 140.0, 180.0),
    POWER(300, 1, 80.0, 300.0),
    ARMOR(400, 4, 80.0, 180.0);

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

    public int getScoreValue() { return scoreValue; }
    public int getMaxHp() { return maxHp; }
    public double getSpeed() { return speed; }
    public double getBulletSpeed() { return bulletSpeed; }
}