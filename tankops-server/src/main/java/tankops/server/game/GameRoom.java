package tankops.server.game;

import java.util.ArrayList;
import java.util.List;

import tankops.server.net.ClientSession;

/**
 * Một phòng chơi. Ở T10 chỉ làm việc giữ danh sách người chơi và giới hạn số người.
 * Sang T15 sẽ thêm GameEngine và vòng tick vào lớp này.
 */
public class GameRoom {

    private final String id;
    private final int maxPlayers;
    private final List<ClientSession> players = new ArrayList<>();

    public GameRoom(String id, int maxPlayers) {
        this.id = id;
        this.maxPlayers = maxPlayers;
    }

    public String getId() {
        return id;
    }

    // synchronized: mỗi lúc chỉ 1 thread được chạy hàm này.
    // Nếu không khóa, 2 thread cùng thấy "còn 1 chỗ" rồi cùng add => phòng bị quá người (TC10).
    public synchronized void addPlayer(ClientSession player) throws RoomFullException {
        if (players.size() >= maxPlayers) {
            throw new RoomFullException("Phòng " + id + " đã đầy (" + maxPlayers + " người)");
        }
        players.add(player);
    }

    public synchronized void removePlayer(ClientSession player) {
        players.remove(player);
    }

    public synchronized int getPlayerCount() {
        return players.size();
    }

    public synchronized boolean isEmpty() {
        return players.isEmpty();
    }
}