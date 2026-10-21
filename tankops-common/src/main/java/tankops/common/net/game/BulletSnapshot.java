package tankops.common.net.game;

import java.util.Objects;
import tankops.common.model.common.Direction;

/**
 * Bản ghi trạng thái của một viên đạn tại một tick cụ thể để truyền qua mạng.
 * Thiết kế bất biến (immutable record).
 */
public record BulletSnapshot(
        String id,
        String ownerId,
        double x,
        double y,
        Direction direction
) {
    public BulletSnapshot {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(ownerId, "ownerId cannot be null");
        Objects.requireNonNull(direction, "direction cannot be null");
    }
}

