package tankops.common.model.powerups;

import tankops.common.engine.GameState;
import tankops.common.model.common.Vector2D;
import tankops.common.model.tank.PlayerTank;

public class Star extends PowerUp {
    public Star(String id, Vector2D position) { super(id, position); }
    @Override public void applyEffect(PlayerTank player, GameState state) {
        player.upgradeStar();
        player.addScore(SCORE_POINTS);
    }
}
