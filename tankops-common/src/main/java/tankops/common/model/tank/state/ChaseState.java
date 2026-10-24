package tankops.common.model.tank.state;


import tankops.common.engine.GameEngine;
import tankops.common.model.common.Direction;
import tankops.common.model.tank.EnemyTank;
import tankops.common.model.tank.PlayerTank;


public class ChaseState implements BotState {

    // 1. Tạo sẵn 1 instance duy nhất (Eager Initialization)
    private static final ChaseState instance = new ChaseState();

    // 2. Private constructor để không ai gọi được 'new PatrolState()'
    private ChaseState() {}

    // 3. Cung cấp hàm lấy instance
    public static ChaseState getInstance() {
        return instance;
    }

    @Override
    public void execute(EnemyTank bot, GameEngine engine) {
        PlayerTank target = engine.findNearestPlayer(bot);
        if (target == null) return;

        double dx = target.getPosition().x() - bot.getPosition().x();
        double dy = target.getPosition().y() - bot.getPosition().y();

        // Hướng chính (trục lệch nhiều hơn) và hướng phụ (trục còn lại)
        Direction primary = Math.abs(dx) > Math.abs(dy)
                ? (dx > 0 ? Direction.RIGHT : Direction.LEFT)
                : (dy > 0 ? Direction.DOWN : Direction.UP);
        Direction secondary = (primary == Direction.LEFT || primary == Direction.RIGHT)
                ? (dy > 0 ? Direction.DOWN : Direction.UP)
                : (dx > 0 ? Direction.RIGHT : Direction.LEFT);

        // Bị tường chặn thì thử đi theo trục còn lại, tránh đứng cọ tường
        if (!engine.moveBot(bot, primary)) {
            engine.moveBot(bot, secondary);
        }
    }
}
