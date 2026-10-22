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
        Map<String, Integer> scores
) implements Message {

    public static final byte TYPE_ID = 0x08;

    public GameOverMessage {
        Objects.requireNonNull(scores, "scores cannot be null");
        scores = Map.copyOf(scores);
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }
}

