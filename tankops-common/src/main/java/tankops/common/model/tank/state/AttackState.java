package tankops.common.model.tank.state;



import tankops.common.engine.GameEngine;
import tankops.common.model.common.Bullet;
import tankops.common.model.common.Direction;
import tankops.common.model.tank.EnemyTank;
import tankops.common.model.tank.PlayerTank;


public class AttackState implements BotState {

    // Độ lệch tối đa (px) giữa hai tâm tank để coi là "thẳng hàng"
    private static final double ALIGN_TOLERANCE = 12.0;
    // 1. Tạo sẵn 1 instance duy nhất (Eager Initialization)
    private static final AttackState instance = new AttackState();

    // 2. Private constructor để không ai gọi được 'new PatrolState()'
    private AttackState() {}

    // 3. Cung cấp hàm lấy instance
    public static AttackState getInstance() {
        return instance;
    }

    @Override
    public void execute(EnemyTank bot, GameEngine engine) {
        PlayerTank target = engine.findNearestPlayer(bot);
        if (target == null) return;

        double dx = target.getPosition().x() - bot.getPosition().x();
        double dy = target.getPosition().y() - bot.getPosition().y();

        if (Math.abs(dy) <= ALIGN_TOLERANCE) {
            // Thẳng hàng ngang -> quay mặt và bắn
            bot.setDirection(dx > 0 ? Direction.RIGHT : Direction.LEFT);
            bot.setMoving(false);
            shoot(bot, engine);
        } else if (Math.abs(dx) <= ALIGN_TOLERANCE) {
            // Thẳng hàng dọc -> quay mặt và bắn
            bot.setDirection(dy > 0 ? Direction.DOWN : Direction.UP);
            bot.setMoving(false);
            shoot(bot, engine);
        } else {
            // Chưa thẳng hàng -> đi theo trục có độ lệch NHỎ hơn để căn hàng
            Direction align = Math.abs(dx) < Math.abs(dy)
                    ? (dx > 0 ? Direction.RIGHT : Direction.LEFT)
                    : (dy > 0 ? Direction.DOWN : Direction.UP);

            if (!engine.moveBot(bot, align)) {
                // Bị tường chặn -> thử trục còn lại
                Direction alt = (align == Direction.LEFT || align == Direction.RIGHT)
                        ? (dy > 0 ? Direction.DOWN : Direction.UP)
                        : (dx > 0 ? Direction.RIGHT : Direction.LEFT);
                engine.moveBot(bot, alt);
            }
        }
    }

    private void shoot(EnemyTank bot, GameEngine engine) {
        if (bot.canShoot()) {
            Bullet bullet = bot.fire(bot.getId() + "_b_" + System.nanoTime());
            if (bullet != null) {
                engine.getGameState().addBullet(bullet);
            }
        }
    }
}

