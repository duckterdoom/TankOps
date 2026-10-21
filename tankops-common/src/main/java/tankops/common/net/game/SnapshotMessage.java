package tankops.common.net.game;

import tankops.common.net.Message;
import java.util.List;
import java.util.Objects;

/**
 * Server → Client: Trạng thái toàn cục của thế giới game tại mỗi nhịp tick (30 Hz).
 * Type ID: 0x07
 */
public record SnapshotMessage(
        int tick,
        List<TankSnapshot> tanks,
        List<BulletSnapshot> bullets,
        boolean gameOver,
        String winnerId
) implements Message {

    public static final byte TYPE_ID = 0x07;

    public SnapshotMessage {
        Objects.requireNonNull(tanks, "tanks cannot be null");
        Objects.requireNonNull(bullets, "bullets cannot be null");
        tanks = List.copyOf(tanks);
        bullets = List.copyOf(bullets);
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }
}

