package tankops.server.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tankops.common.map.GameMap;
import tankops.server.net.ClientSession;

/**
 * Quản lý tất cả phòng: tạo phòng, liệt kê phòng, vào phòng, rời phòng, bắt đầu ván.
 * Giới hạn số phòng (server.maxRooms) và số người mỗi phòng (room.maxPlayers).
 *
 * Phòng có sẵn: server luôn cố giữ ít nhất minLobbyRooms phòng đang chờ (LOBBY),
 * để người chơi vào lobby là thấy phòng ngay, không phải tự tạo.
 */
public class RoomManager {

    // Mã đặc biệt: client gửi mã này khi muốn "tạo phòng mới rồi vào luôn"
    public static final String NEW_ROOM = "new";
    // Mã đặc biệt: client gửi mã này (hoặc để trống) khi muốn "vào phòng nào còn chỗ cũng được"
    public static final String AUTO = "auto";

    private final int maxRooms;
    private final int maxPlayersPerRoom;
    private final int minLobbyRooms;

    // LinkedHashMap giữ đúng thứ tự tạo phòng khi liệt kê
    private final Map<String, GameRoom> rooms = new LinkedHashMap<>();
    private int nextRoomNumber = 1;

    // Bản cũ (2 tham số): không có phòng tạo sẵn. Giữ lại để các test cũ vẫn chạy.
    public RoomManager(int maxRooms, int maxPlayersPerRoom) {
        this(maxRooms, maxPlayersPerRoom, 0);
    }

    public RoomManager(int maxRooms, int maxPlayersPerRoom, int minLobbyRooms) {
        this.maxRooms = maxRooms;
        this.maxPlayersPerRoom = maxPlayersPerRoom;
        this.minLobbyRooms = minLobbyRooms;
        ensureLobbyRooms(); // tạo sẵn các phòng ngay khi khởi động
    }

    // Mọi hàm public dưới đây đều synchronized: mỗi lúc chỉ 1 thread được thay đổi danh sách phòng.
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

    public synchronized GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public synchronized void joinRoom(String roomId, ClientSession player) throws RoomFullException {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Không có phòng " + roomId);
        }
        addToRoom(room, player);
    }

    /**
     * Vào phòng theo yêu cầu của client, làm trọn "tìm/tạo phòng + thêm người" trong MỘT khối khóa.
     * (Nếu tách làm 2 bước, phòng có thể bị xóa chen vào giữa => người chơi vào "phòng ma".)
     *  - roomId là "new": tạo phòng mới rồi vào.
     *  - roomId là "auto" hoặc trống: vào phòng đang chờ còn chỗ, không có thì tạo mới.
     *  - roomId khác: phải là phòng đang có, không có thì báo lỗi (không tự tạo phòng khác).
     */
    public synchronized GameRoom joinOrCreate(String roomId, ClientSession player) throws RoomFullException {
        GameRoom room;
        if (roomId == null || roomId.isBlank() || AUTO.equals(roomId)) {
            room = findWaitingRoomWithSpace();
            if (room == null) {
                room = createRoom();
            }
        } else if (NEW_ROOM.equals(roomId)) {
            room = createRoom();
        } else {
            room = rooms.get(roomId);
            if (room == null) {
                throw new IllegalArgumentException("Không có phòng " + roomId);
            }
        }
        addToRoom(room, player);
        return room;
    }

    // Phòng rỗng thì xóa, trừ khi đó là phòng chờ cần giữ cho đủ số phòng có sẵn
    public synchronized void leaveRoom(String roomId, ClientSession player) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            return;
        }
        room.removePlayer(player);
        if (room.isEmpty()) {
            boolean neededAsDefault = room.getState() == RoomState.LOBBY
                    && countLobbyRooms() <= minLobbyRooms;
            if (!neededAsDefault) {
                rooms.remove(roomId);
            }
        }
        ensureLobbyRooms();
    }

    /**
     * Bắt đầu ván trong phòng. Chỉ người đang ở trong phòng đó mới được bắt đầu.
     * Sau khi phòng chuyển sang PLAYING, tự tạo thêm phòng chờ mới để lobby luôn có phòng.
     */
    public synchronized void startGame(String roomId, ClientSession requester, GameMap map, int tickRate) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Không có phòng " + roomId);
        }
        if (requester.getCurrentRoom() != room) {
            throw new IllegalStateException("Bạn không ở trong phòng " + roomId);
        }
        room.startGame(map, tickRate); // ném IllegalStateException nếu không ở LOBBY hoặc chưa đủ người
        ensureLobbyRooms();
    }

    // ---- hàm nội bộ (luôn được gọi khi đang giữ khóa của RoomManager) ----

    private void addToRoom(GameRoom room, ClientSession player) throws RoomFullException {
        GameRoom current = player.getCurrentRoom();
        if (current == room) {
            // Bấm vào lại đúng phòng mình đang ở (ví dụ vừa chơi xong rồi quay lại): coi như thành công.
            // Phòng đã kết thúc thì đưa về phòng chờ để chơi lại.
            room.resetToLobbyIfFinished();
            return;
        }
        if (current != null) {
            throw new IllegalStateException("Bạn đang ở trong phòng " + current.getId());
        }
        room.addPlayer(player);
    }

    private GameRoom findWaitingRoomWithSpace() {
        for (GameRoom room : rooms.values()) {
            if (room.getState() == RoomState.LOBBY && room.getPlayerCount() < room.getMaxPlayers()) {
                return room;
            }
        }
        return null;
    }

    private int countLobbyRooms() {
        int count = 0;
        for (GameRoom room : rooms.values()) {
            if (room.getState() == RoomState.LOBBY) {
                count++;
            }
        }
        return count;
    }

    // Tạo thêm phòng chờ cho đến khi đủ minLobbyRooms (không vượt maxRooms)
    private void ensureLobbyRooms() {
        while (countLobbyRooms() < minLobbyRooms && rooms.size() < maxRooms) {
            createRoom();
        }
    }
}