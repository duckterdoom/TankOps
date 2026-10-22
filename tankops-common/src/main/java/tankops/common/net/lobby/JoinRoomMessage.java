package tankops.common.net.lobby;

import tankops.common.net.Message;
import java.util.Objects;

/**
 * Client → Server: Yêu cầu vào một phòng chơi (hoặc tạo mới nếu chưa có).
 * Type ID: 0x01
 */
public record JoinRoomMessage(
        String roomId,
        String playerName
) implements Message {

    public static final byte TYPE_ID = 0x01;

    public JoinRoomMessage {
        Objects.requireNonNull(roomId, "roomId cannot be null");
        Objects.requireNonNull(playerName, "playerName cannot be null");
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }
}

