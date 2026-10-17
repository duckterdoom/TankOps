package tankops.server.game;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import tankops.common.engine.GameEngine;
import tankops.common.engine.GameState;
import tankops.common.map.GameMap;
import tankops.common.model.common.Direction;
import tankops.common.model.tank.Tank;
import tankops.server.net.ClientSession;

/**
 * Một phòng chơi: giữ danh sách người chơi (T10) + chạy vòng tick của game (T15).
 *
 * QUY TẮC VÀNG (single-writer): chỉ có thread của TickLoop được gọi GameEngine.
 * Các thread socket chỉ BỎ LỆNH vào hàng đợi (submitMove). Đến lượt tick, ta lấy lệnh ra xử lý.
 */
public class GameRoom {

    private final String id;
    private final int maxPlayers;
    private final List<ClientSession> players = new ArrayList<>();

    // ---- phần T15 ----
    // Hộp thư lệnh di chuyển. ConcurrentLinkedQueue an toàn khi nhiều thread cùng bỏ/lấy.
    private final ConcurrentLinkedQueue<MoveCommand> inputQueue = new ConcurrentLinkedQueue<>();
    private GameEngine engine;          // chỉ thread tick được đụng vào
    private TickLoop tickLoop;
    private volatile int tickCount = 0; // volatile để thread khác đọc được giá trị mới nhất

    // Một lệnh di chuyển: ai, hướng nào (direction = null nghĩa là đứng yên)
    private record MoveCommand(String playerId, Direction direction) { }

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

    // ================= T15 =================

    /** Bắt đầu ván: tạo engine, đặt xe cho người chơi thật (không bot), chạy vòng tick. */
    public synchronized void startGame(GameMap map, int tickRate) {
        if (tickLoop != null) {
            return; // đã bắt đầu rồi
        }
        engine = new GameEngine(map);

        List<String> ids = new ArrayList<>();
        for (ClientSession p : players) {
            ids.add(p.getPlayerId());
        }
        engine.spawnPlayers(ids, map.getPlayerSpawnPoints());

        tickLoop = new TickLoop(tickRate);
        tickLoop.start(this::tick);
    }

    public synchronized void stopGame() {
        if (tickLoop != null) {
            tickLoop.stop();
            tickLoop = null;
        }
    }

    /** Thread socket gọi hàm này khi nhận lệnh của client. Chỉ bỏ vào hàng đợi, KHÔNG đụng engine. */
    public void submitMove(String playerId, Direction direction) {
        inputQueue.add(new MoveCommand(playerId, direction));
    }

    public int getTickCount() {
        return tickCount;
    }

    /** 1 nhịp tick: (1) lấy lệnh  (2) chạy engine  (3) gửi trạng thái cho mọi người. */
    private void tick() {
        // (1) Lấy hết lệnh đang chờ, đưa cho engine
        MoveCommand cmd;
        while ((cmd = inputQueue.poll()) != null) {
            engine.handleMoveEvent(cmd.playerId(), cmd.direction());
        }

        // (2) Engine cập nhật 1 bước
        engine.update();
        tickCount++;

        // (3) Gửi cho từng client
        broadcast(buildSnapshot());
    }

    // Tạm thời là chuỗi chữ đơn giản. Khi M1 xong protocol sẽ đổi sang Message/MessageCodec.
    private String buildSnapshot() {
        GameState state = engine.getGameState();
        StringBuilder sb = new StringBuilder("STATE tick=" + tickCount);
        for (Tank t : state.getTanks()) {
            sb.append(" ").append(t.getId())
                    .append("@").append((int) t.getPosition().x())
                    .append(",").append((int) t.getPosition().y());
        }
        return sb.toString();
    }

    private void broadcast(String message) {
        // Copy danh sách dưới khóa rồi gửi NGOÀI khóa: gửi mạng có thể chậm,
        // không nên giữ khóa phòng trong lúc đó.
        List<ClientSession> copy;
        synchronized (this) {
            copy = new ArrayList<>(players);
        }
        for (ClientSession p : copy) {
            try {
                p.send(message);
            } catch (Exception e) {
                // 1 client lỗi thì bỏ qua, vẫn gửi tiếp cho người khác
                System.out.println("[GameRoom] Không gửi được cho " + p.getPlayerId());
            }
        }
    }
}