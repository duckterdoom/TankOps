package tankops.common.model.tank.state;

import tankops.common.engine.GameEngine;
import tankops.common.model.tank.EnemyTank;

public interface BotState {
    void execute(EnemyTank bot, GameEngine engine);
}
