package tankops.common.net.game;

import tankops.common.net.Message;
import java.util.Objects;

/**
 * Server → Client: Thông báo các sự kiện rời rạc trong trận đấu.
 * Type ID: 0x09
 */
public record EventMessage(
        EventType eventType,
        String sourceId,
        String targetId,
        double x,
        double y
) implements Message {

    public static final byte TYPE_ID = 0x09;

    public enum EventType {
        TANK_HIT,           // đạn trúng xe
        TANK_DESTROYED,     // xe bị phá hủy hoàn toàn
        BULLET_BLOCKED,     // đạn bị tường thép chặn lại
        WALL_DESTROYED,     // tường gạch bị phá vỡ
        POWERUP_SPAWNED,    // vật phẩm xuất hiện
        POWERUP_COLLECTED   // người chơi nhặt vật phẩm
    }

    public EventMessage {
        Objects.requireNonNull(eventType, "eventType cannot be null");
        Objects.requireNonNull(sourceId, "sourceId cannot be null");
        Objects.requireNonNull(targetId, "targetId cannot be null");
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }
}

