package tankops.common.net.lobby;

import java.util.Objects;

/**
 * Bản ghi chứa thông tin tóm tắt của một phòng chơi, gửi trong RoomListResponseMessage.
 */
public record RoomInfo(
        String roomId,
        int currentPlayers,
        int maxPlayers,
        String state
) {
    public RoomInfo {
        Objects.requireNonNull(roomId, "roomId cannot be null");
        Objects.requireNonNull(state, "state cannot be null");
    }
}

