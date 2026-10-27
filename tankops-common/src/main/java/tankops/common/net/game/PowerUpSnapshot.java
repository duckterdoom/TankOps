package tankops.common.net.game;

import java.util.Objects;

/**
 * Trạng thái một vật phẩm (power-up) đang nằm trên bản đồ, để truyền qua mạng.
 * kind là tên loại vật phẩm: "Helmet", "Grenade", "Star", "TankPowerUp", "Timer", "Shovel".
 * Thiết kế bất biến (immutable record).
 */
public record PowerUpSnapshot(
        String id,
        String kind,
        double x,
        double y
) {
    public PowerUpSnapshot {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(kind, "kind cannot be null");
    }
}
