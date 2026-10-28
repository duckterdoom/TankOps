package tankops.common.model.powerups;

import tankops.common.engine.GameState;
import tankops.common.model.common.Vector2D;
import tankops.common.model.tank.PlayerTank;
import tankops.common.model.tank.Tank;

public class Grenade extends PowerUp {
    public Grenade(String id, Vector2D position) { super(id, position); }
    @Override public void applyEffect(PlayerTank player, GameState state) {

        player.addScore(SCORE_POINTS);
        for(Tank tank : state.getTanks()) {
            if(!tank.isDestroyed() && !tank.getId().equals(player.getId())) {
                tank.takeDamage(999);
            }
        }
    }
}
