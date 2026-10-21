package tankops.common.net.game;

import java.util.Objects;
import tankops.common.model.common.Direction;

/**
 * Bản ghi trạng thái của một xe tăng tại một tick cụ thể để truyền qua mạng.
 * Thiết kế bất biến (immutable record).
 */
public record TankSnapshot(
        String id,
        double x,
        double y,
        Direction direction,
        int health,
        int maxHealth,
        int starLevel,
        boolean shielded,
        boolean moving,
        boolean destroyed
) {
    public TankSnapshot {
        Objects.requireNonNull(id, "id cannot be null");
        if (maxHealth <= 0) {
            throw new IllegalArgumentException("maxHealth must be > 0");
        }
    }
}

