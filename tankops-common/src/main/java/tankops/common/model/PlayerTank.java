package tankops.common.model;

public class PlayerTank extends Tank {
    private final int playerNumber;
    private int starLevel;
    private int lives;

    public PlayerTank(String id, Vector2D position, Direction direction, int playerNumber) {
        super(id, position, direction, 120.0, 180.0, 1);
        this.playerNumber = playerNumber;
        this.starLevel = 0;
        this.lives = 3;
    }

    public int getPlayerNumber() { return playerNumber; }
    public int getStarLevel() { return starLevel; }
    public int getLives() { return lives; }

    public void upgradeStar() {
        if (starLevel < 3) {
            starLevel++;
            if (starLevel == 1) {
                this.bulletSpeed = 300.0;
            }
        }
    }

    public void resetPowerLevel() {
        this.starLevel = 0;
        this.bulletSpeed = 180.0;
    }

    public void addLife() { this.lives++; }

    public void loseLife() {
        if (this.lives > 0) this.lives--;
    }
}