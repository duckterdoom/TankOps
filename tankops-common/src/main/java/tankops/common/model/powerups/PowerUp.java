package tankops.common.model.powerups;


import tankops.common.model.common.Entity;
import tankops.common.model.common.Vector2D;
import tankops.common.model.tank.PlayerTank;

public abstract class PowerUp extends Entity {
    public static final double POWERUP_SIZE = 32.0;
    public static final int SCORE_POINTS = 500;

    public PowerUp(String id, Vector2D position) {
        super(id, position, POWERUP_SIZE, POWERUP_SIZE);
    }

    public abstract void applyEffect(PlayerTank player);
}

