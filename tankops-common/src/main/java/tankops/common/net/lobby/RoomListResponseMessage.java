package tankops.common.net.lobby;

import tankops.common.net.Message;
import java.util.List;
import java.util.Objects;

/**
 * Server → Client: Trả về danh sách các phòng chơi hiện có.
 * Type ID: 0x05
 */
public record RoomListResponseMessage(
        List<RoomInfo> rooms
) implements Message {

    public static final byte TYPE_ID = 0x05;

    public RoomListResponseMessage {
        Objects.requireNonNull(rooms, "rooms cannot be null");
        rooms = List.copyOf(rooms);
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }
}

