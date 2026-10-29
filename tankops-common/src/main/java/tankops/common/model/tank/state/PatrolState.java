package tankops.common.model.tank.state;

import tankops.common.engine.GameEngine;
import tankops.common.model.tank.EnemyTank;

public class PatrolState implements BotState {
    // 1. Tạo sẵn 1 instance duy nhất (Eager Initialization)
    private static final PatrolState instance = new PatrolState();

    // 2. Private constructor để không ai gọi được 'new PatrolState()'
    private PatrolState() {}

    // 3. Cung cấp hàm lấy instance
    public static PatrolState getInstance() {
        return instance;
    }

    @Override
    public void execute(EnemyTank bot, GameEngine engine) {

        boolean moved = engine.moveBot(bot, bot.getDirection());
        if (!moved || Math.random() < 0.02) {
            bot.setDirection(engine.getRandomDirection());
        }
    }

}
