package tankops.common.model;

public class PlayerTank extends Tank {
    private static final int MAX_HP = 1;
    private static final int INITIAL_LIVES = 3;
    private static final int EXTRA_LIFE_SCORE = 20_000;

    private final int playerNumber;
    private int starLevel = 0;
    private int lives = INITIAL_LIVES;
    private int score = 0;
    private boolean scoreLifeGranted = false;

    public PlayerTank(String id, Vector2D position, Direction direction, int playerNumber) {
        super(id, position, direction, MOVE_NORMAL, MAX_HP);
        this.playerNumber = playerNumber;
    }

    public int getPlayerNumber()  { return playerNumber; }
    public int getStarLevel()     { return starLevel; }
    public int getLives()         { return lives; }
    public int getScore()         { return score; }
    public boolean hasLivesLeft() { return lives > 0; }

    // Mọi năng lực suy ra từ starLevel, không lưu trạng thái trùng lặp.
    @Override public double getBulletSpeed() {
        return starLevel >= 1 ? Bullet.SPEED_FAST : Bullet.SPEED_SLOW;
    }
    @Override public int getMaxActiveBullets() { return starLevel >= 2 ? 2 : 1; }
    @Override public int getBulletStarLevel()  { return starLevel; }

    public void upgradeStar() {
        if (starLevel < Bullet.MAX_STAR_LEVEL) starLevel++;
    }

    /** Gọi khi xe chết: power level về mặc định. */
    public void resetPowerLevel() { starLevel = 0; }

    public void addLife()  { lives++; }
    public void loseLife() { if (lives > 0) lives--; }

    /** Cộng điểm; đạt 20.000 điểm lần đầu thì được thêm 1 mạng. */
    public void addScore(int points) {
        if (points <= 0) return;
        score += points;
        if (!scoreLifeGranted && score >= EXTRA_LIFE_SCORE) {
            scoreLifeGranted = true;
            addLife();
        }
    }
}
