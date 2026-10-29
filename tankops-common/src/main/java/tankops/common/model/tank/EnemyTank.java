package tankops.common.model.tank;


import tankops.common.model.common.Direction;
import tankops.common.model.common.Vector2D;
import tankops.common.model.tank.state.BotState;
import tankops.common.model.tank.state.PatrolState;

public abstract class EnemyTank extends Tank {
    private final int points;
    private boolean frozen = false;
    private BotState currentState;
    private int freezeTicksRemaining = 0; // Thêm bộ đếm thời gian đóng băng
    private static final int FREEZE_DURATION_TICKS = 300;

    public EnemyTank(String id, Vector2D position, Direction direction, double moveSpeed, int maxHealth, int points) {
        super(id, position, direction, moveSpeed, maxHealth);
        this.points = points;
        this.currentState = PatrolState.getInstance();
    }

    public int getPoints() { return points; }
    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }

    // Kích hoạt đóng băng và gán thời gian
    public void freeze() {
        this.frozen = true;
        this.freezeTicksRemaining = FREEZE_DURATION_TICKS;
        System.out.println("Froze ticks remaining: " + this.freezeTicksRemaining);
    }

    // Cập nhật đếm ngược thời gian đóng băng mỗi tick
    public void updateFreeze() {
        if (frozen) {
            freezeTicksRemaining--;
            if (freezeTicksRemaining <= 0) {
                frozen = false;
            }
        }

    }
    @Override
    public int getBulletStarLevel() { return 0; }

    public BotState getCurrentState() {
        return currentState;
    }

    public void changeState(BotState newState) {
        this.currentState = newState;
    }
}

