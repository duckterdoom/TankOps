package tankops.common.model.powerups;

import tankops.common.model.common.Vector2D;
import tankops.common.model.tank.PlayerTank;

public class Helmet extends PowerUp {
    public Helmet(String id, Vector2D position) { super(id, position); }
    @Override public void applyEffect(PlayerTank player) {
        player.activateShield();
        player.addScore(SCORE_POINTS);
    }
}
