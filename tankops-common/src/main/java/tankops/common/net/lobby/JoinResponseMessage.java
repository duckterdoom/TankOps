package tankops.common.net.lobby;

import tankops.common.net.Message;
import java.util.Objects;

/**
 * Server → Client: Phản hồi kết quả yêu cầu vào phòng.
 * Type ID: 0x02
 */
public record JoinResponseMessage(
        boolean success,
        String message,
        String roomId,
        String assignedPlayerId,
        int playerNumber
) implements Message {

    public static final byte TYPE_ID = 0x02;

    public JoinResponseMessage {
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(roomId, "roomId cannot be null");
        Objects.requireNonNull(assignedPlayerId, "assignedPlayerId cannot be null");
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }
}

