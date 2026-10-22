package tankops.common.net.lobby;

import tankops.common.net.Message;
import java.util.Objects;

/**
 * Client → Server: Yêu cầu rời khỏi phòng chơi hiện tại.
 * Type ID: 0x03
 */
public record LeaveRoomMessage(
        String roomId,
        String playerId
) implements Message {

    public static final byte TYPE_ID = 0x03;

    public LeaveRoomMessage {
        Objects.requireNonNull(roomId, "roomId cannot be null");
        Objects.requireNonNull(playerId, "playerId cannot be null");
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }
}

