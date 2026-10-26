package tankops.common.model.powerups;

import tankops.common.engine.GameState;
import tankops.common.model.common.Vector2D;
import tankops.common.model.tank.EnemyTank;
import tankops.common.model.tank.PlayerTank;
import tankops.common.model.tank.Tank;

public class Timer extends PowerUp {
    public Timer(String id, Vector2D position) { super(id, position); }
    @Override public void applyEffect(PlayerTank player, GameState state) {
        player.addScore(SCORE_POINTS);
        // Đóng băng toàn bộ EnemyTank đang hoạt động trên màn hình
        if (state.getTanks() != null) {
            for (Tank tank : state.getTanks()) {
                if (tank instanceof EnemyTank enemyBot && !enemyBot.isDestroyed()) {
                    enemyBot.freeze();
                }
            }
        }
    }
}
