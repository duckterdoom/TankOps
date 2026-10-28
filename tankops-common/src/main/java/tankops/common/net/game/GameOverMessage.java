package tankops.common.net.game;

import tankops.common.net.Message;
import java.util.Map;
import java.util.Objects;

/**
 * Server → Client: Thông báo ván đấu kết thúc kèm bảng điểm tổng kết.
 * Type ID: 0x08
 */
public record GameOverMessage(
        String winnerId,
        Map<String, Integer> scores,
        Map<String, Integer> kills,
        Map<String, Integer> deaths
) implements Message {

    public static final byte TYPE_ID = 0x08;

    public GameOverMessage {
        Objects.requireNonNull(scores, "scores cannot be null");
        scores = Map.copyOf(scores);
        kills = kills != null ? Map.copyOf(kills) : Map.of();
        deaths = deaths != null ? Map.copyOf(deaths) : Map.of();
    }

    // Bản cũ (chỉ có điểm): giữ lại để code và test cũ vẫn chạy; số kill và death để trống
    public GameOverMessage(String winnerId, Map<String, Integer> scores) {
        this(winnerId, scores, Map.of(), Map.of());
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }
}

