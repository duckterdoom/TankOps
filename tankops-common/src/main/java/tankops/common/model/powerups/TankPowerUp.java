package tankops.common.model.powerups;

import tankops.common.model.common.Vector2D;
import tankops.common.model.tank.PlayerTank;

public class TankPowerUp extends PowerUp {
    public TankPowerUp(String id, Vector2D position) { super(id, position); }
    @Override public void applyEffect(PlayerTank player) {
        player.addLife();
        player.addScore(SCORE_POINTS);
    }
}
