package tankops.server.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tankops.server.net.ClientSession;

/**
 * Quản lý tất cả phòng: tạo phòng, liệt kê phòng, vào phòng, rời phòng.
 * Giới hạn số phòng (server.maxRooms) và số người mỗi phòng (room.maxPlayers).
 */
public class RoomManager {

    private final int maxRooms;
    private final int maxPlayersPerRoom;

    // LinkedHashMap giữ đúng thứ tự tạo phòng khi liệt kê
    private final Map<String, GameRoom> rooms = new LinkedHashMap<>();
    private int nextRoomNumber = 1;

    public RoomManager(int maxRooms, int maxPlayersPerRoom) {
        this.maxRooms = maxRooms;
        this.maxPlayersPerRoom = maxPlayersPerRoom;
    }

    // Mọi hàm dưới đây đều synchronized: mỗi lúc chỉ 1 thread được thay đổi danh sách phòng.
    // Thứ tự khóa luôn là RoomManager rồi mới tới GameRoom.
    // Vì vậy trong GameRoom đừng bao giờ gọi ngược lại RoomManager (sẽ bị đứng máy - deadlock).

    public synchronized GameRoom createRoom() {
        if (rooms.size() >= maxRooms) {
            throw new IllegalStateException("Đã đạt số phòng tối đa (" + maxRooms + ")");
        }
        String id = "room-" + nextRoomNumber;
        nextRoomNumber++;
        GameRoom room = new GameRoom(id, maxPlayersPerRoom);
        rooms.put(id, room);
        return room;
    }

    // Trả về bản sao để bên ngoài không sửa trực tiếp vào danh sách thật
    public synchronized List<GameRoom> listRooms() {
        return new ArrayList<>(rooms.values());
    }

    public synchronized void joinRoom(String roomId, ClientSession player) throws RoomFullException {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Không có phòng " + roomId);
        }
        room.addPlayer(player);
    }

    // Phòng không còn ai thì xóa luôn
    public synchronized void leaveRoom(String roomId, ClientSession player) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            return;
        }
        room.removePlayer(player);
        if (room.isEmpty()) {
            rooms.remove(roomId);
        }
    }
}