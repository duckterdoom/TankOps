package tankops.common.net.lobby;

import tankops.common.net.Message;
import java.util.Objects;

/**
 * Client → Server: Yêu cầu lấy danh sách các phòng đang mở.
 * Type ID: 0x04
 */
public record RoomListRequestMessage(
        String requesterId
) implements Message {

    public static final byte TYPE_ID = 0x04;

    public RoomListRequestMessage {
        Objects.requireNonNull(requesterId, "requesterId cannot be null");
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }
}

