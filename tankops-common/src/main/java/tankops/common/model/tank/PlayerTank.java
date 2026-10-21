package tankops.common.model.tank;


import tankops.common.model.common.Bullet;
import tankops.common.model.common.Direction;
import tankops.common.model.common.Vector2D;

public class PlayerTank extends Tank {
    private static final int INITIAL_LIVES = 1;
    private static final int EXTRA_LIFE_SCORE = 20_000;

    private final int playerNumber;
    private int starLevel = 0;
    private int lives = INITIAL_LIVES;
    private int score = 0;
    private boolean scoreLifeGranted = false;
    private boolean shielded = false;

    private final Vector2D spawnPoint;

    public PlayerTank(String id, Vector2D position, Direction direction, int playerNumber) {
        super(id, position, direction, MOVE_NORMAL, 1);
        this.playerNumber = playerNumber;
        this.spawnPoint = position;
    }

    public int getPlayerNumber() { return playerNumber; }
    public int getStarLevel() { return starLevel; }
    public int getLives() { return lives; }
    public int getScore() { return score; }
    public boolean isShielded() { return shielded; }

    public void activateShield() { this.shielded = true; }
    public void deactivateShield() { this.shielded = false; }

    @Override
    public void takeDamage(int amount) {
        if (shielded) return;
        super.takeDamage(amount);

        if(getHealth() <= 0){
            loseLife();
            resetPowerLevel();

            if(lives > 0 ){
                respawn();
            }
        }
    }

    public void respawn(){
        setHealth(getMaxHealth());
        setPosition(spawnPoint);
    }
    @Override
    public boolean isDestroyed() {
        return lives <= 0 && getHealth() <= 0;
    }

    @Override
    public double getBulletSpeed() {
        return starLevel >= 1 ? Bullet.SPEED_FAST : Bullet.SPEED_SLOW;
    }

    @Override
    public int getBulletStarLevel() { return starLevel; }

    public int getMaxActiveBullets() { return starLevel >= 2 ? 2 : 1; }

    public void upgradeStar() {
        if (starLevel < Bullet.MAX_STAR_LEVEL) starLevel++;
    }

    public void resetPowerLevel() { starLevel = 0; }

    public void addLife() { lives++; }
    public void loseLife() { if (lives > 0) lives--; }

    public void addScore(int points) {
        if (points <= 0) return;
        score += points;
        if (!scoreLifeGranted && score >= EXTRA_LIFE_SCORE) {
            scoreLifeGranted = true;
            addLife();
        }
    }
}
