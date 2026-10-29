package tankops.common.net.game;

import java.util.Objects;

/**
 * Trạng thái một ô tường THAY ĐỔI ĐƯỢC (gạch hoặc thép) để truyền qua mạng.
 * kind là "BrickWall" hoặc "SteelWall". Tường đã bị phá thì không còn trong danh sách.
 * Nước, cây, băng không bao giờ đổi nên client tự đọc từ file bản đồ.
 * Thiết kế bất biến (immutable record).
 */
public record EnvironmentSnapshot(
        String id,
        String kind,
        double x,
        double y
) {
    public EnvironmentSnapshot {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(kind, "kind cannot be null");
    }
}
