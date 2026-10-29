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
        String winnerId,
        List<PowerUpSnapshot> powerUps,
        List<EnvironmentSnapshot> environments,
        List<String> sounds
) implements Message {

    public static final byte TYPE_ID = 0x07;

    public SnapshotMessage {
        Objects.requireNonNull(tanks, "tanks cannot be null");
        Objects.requireNonNull(bullets, "bullets cannot be null");
        tanks = List.copyOf(tanks);
        bullets = List.copyOf(bullets);
        powerUps = powerUps != null ? List.copyOf(powerUps) : List.of();
        environments = environments != null ? List.copyOf(environments) : List.of();
        // Tên file âm thanh xảy ra trong tick này (vd "shoot.wav"); client phát đúng 1 lần khi nhận
        sounds = sounds != null ? List.copyOf(sounds) : List.of();
    }

    // Bản 7 tham số (chưa có âm thanh): giữ lại để code và test cũ vẫn chạy
    public SnapshotMessage(int tick, List<TankSnapshot> tanks, List<BulletSnapshot> bullets,
                           boolean gameOver, String winnerId,
                           List<PowerUpSnapshot> powerUps, List<EnvironmentSnapshot> environments) {
        this(tick, tanks, bullets, gameOver, winnerId, powerUps, environments, List.of());
    }

    // Bản cũ (5 tham số, không có vật phẩm và tường): giữ lại để code và test cũ vẫn chạy
    public SnapshotMessage(int tick, List<TankSnapshot> tanks, List<BulletSnapshot> bullets,
                           boolean gameOver, String winnerId) {
        this(tick, tanks, bullets, gameOver, winnerId, List.of(), List.of(), List.of());
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }
}

