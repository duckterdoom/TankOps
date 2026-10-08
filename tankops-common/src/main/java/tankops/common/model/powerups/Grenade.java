package tankops.common.model.powerups;

import tankops.common.model.common.Vector2D;
import tankops.common.model.tank.PlayerTank;

public class Grenade extends PowerUp {
    public Grenade(String id, Vector2D position) { super(id, position); }
    @Override public void applyEffect(PlayerTank player) { player.addScore(SCORE_POINTS); }
}
