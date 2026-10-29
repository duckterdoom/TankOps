package tankops.common.net.lobby;

import tankops.common.net.Message;
import java.util.List;
import java.util.Objects;

/**
 * Server -> All Clients in Room: Cập nhật danh sách người chơi thực tế trong phòng.
 * Type ID: 0x0E
 */
public record RoomStateMessage(
        String roomId,
        List<String> playerNames,  // Danh sách tên tất cả người chơi (Vi trí 0 = Host)
        String hostId,
        List<String> readyPlayerNames
) implements Message {

    public static final byte TYPE_ID = 0x0E;

    public RoomStateMessage {
        Objects.requireNonNull(roomId, "roomId cannot be null");
        Objects.requireNonNull(playerNames, "playerNames cannot be null");
        playerNames = List.copyOf(playerNames);
        readyPlayerNames = readyPlayerNames != null ? List.copyOf(readyPlayerNames) : List.of();
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }
}
