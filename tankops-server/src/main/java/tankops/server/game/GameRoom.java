package tankops.server.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import tankops.common.engine.GameEngine;
import tankops.common.engine.GameState;
import tankops.common.map.GameMap;
import tankops.common.model.common.Bullet;
import tankops.common.model.common.Direction;
import tankops.common.model.tank.PlayerTank;
import tankops.common.model.tank.Tank;
import tankops.common.net.Message;
import tankops.common.net.game.BulletSnapshot;
import tankops.common.net.game.GameOverMessage;
import tankops.common.net.game.SnapshotMessage;
import tankops.common.net.game.TankSnapshot;
import tankops.server.net.ClientSession;

/**
 * Một phòng chơi: danh sách người chơi (T10) + vòng tick (T15) + vòng đời và điểm số (T21).
 *
 * QUY TẮC VÀNG (single-writer): chỉ có thread của TickLoop được gọi GameEngine.
 * Các thread socket chỉ BỎ LỆNH vào hàng đợi (submitMove, submitShoot) hoặc bật cờ (requestFinish).
 */
public class GameRoom {

    // Ván đấu cần ít nhất 2 người (chỉ 1 người thì không có ai để đánh)
    public static final int MIN_PLAYERS_TO_START = 2;
    // Điểm cộng cho mỗi lần hạ 1 xe địch (con số tạm, nhóm có thể đổi)
    public static final int KILL_POINTS = 100;

    private final String id;
    private final int maxPlayers;
    private final List<ClientSession> players = new ArrayList<>();

    // ---- T21: vòng đời + điểm ----
    // volatile: thread tick đọc được ngay giá trị mới nhất. Việc GHI luôn nằm trong synchronized.
    private volatile RoomState state = RoomState.LOBBY;
    private volatile boolean finishRequested = false; // thread khác bật cờ này để xin kết thúc ván
    private final Map<String, Integer> scores = new LinkedHashMap<>(); // playerId -> điểm
    private String winnerId = null; // null = chưa xong hoặc hòa

    // ---- T15: tick ----
    private final ConcurrentLinkedQueue<InputCommand> inputQueue = new ConcurrentLinkedQueue<>();
    private GameEngine engine;          // chỉ thread tick được đụng vào
    private TickLoop tickLoop;
    private volatile int tickCount = 0;

    // Một lệnh của người chơi: hoặc đổi hướng đi (shoot = false), hoặc bắn (shoot = true)
    private record InputCommand(String playerId, Direction direction, boolean shoot) { }

    public GameRoom(String id, int maxPlayers) {
        this.id = id;
        this.maxPlayers = maxPlayers;
    }

    public String getId() {
        return id;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    // ================= Danh sách người chơi (T10) =================

    // Chỉ cho vào khi phòng còn ở LOBBY. Phòng đã PLAYING/FINISHED thì từ chối.
    // synchronized: "kiểm tra trạng thái + kiểm tra chỗ + thêm người" là một khối không bị chen ngang.
    public synchronized void addPlayer(ClientSession player) throws RoomFullException {
        if (state != RoomState.LOBBY) {
            throw new IllegalStateException("Phòng " + id + " đã bắt đầu, không thể vào");
        }
        if (players.size() >= maxPlayers) {
            throw new RoomFullException("Phòng " + id + " đã đầy (" + maxPlayers + " người)");
        }
        players.add(player);
        player.setCurrentRoom(this);
    }

    public synchronized void removePlayer(ClientSession player) {
        players.remove(player);
        if (player.getCurrentRoom() == this) {
            player.setCurrentRoom(null);
        }

        if (state == RoomState.PLAYING) {
            if (players.isEmpty()) {
                stopGame();              // không còn ai: tắt vòng tick, khỏi chạy vô ích
            } else if (players.size() < MIN_PLAYERS_TO_START) {
                finishRequested = true;  // còn 1 người: xin kết thúc ván (thread tick sẽ xử lý)
            }
        }
    }

    public synchronized int getPlayerCount() {
        return players.size();
    }

    public synchronized boolean isEmpty() {
        return players.isEmpty();
    }

    // ================= Vòng đời (T21) =================

    public RoomState getState() {
        return state;
    }

    /** LOBBY -> PLAYING: tạo engine, đặt xe cho người chơi thật (không bot), chạy vòng tick. */
    public synchronized void startGame(GameMap map, int tickRate) {
        if (state != RoomState.LOBBY) {
            throw new IllegalStateException("Phòng " + id + " không ở trạng thái LOBBY");
        }
        if (players.size() < MIN_PLAYERS_TO_START) {
            throw new IllegalStateException("Cần ít nhất " + MIN_PLAYERS_TO_START + " người để bắt đầu");
        }

        engine = new GameEngine(map);
        // Engine báo "có người hạ xe" -> cộng điểm cho người bắn. (Hàm này chạy trên thread tick)
        engine.setOnKillCallback(this::addKill);

        List<String> ids = new ArrayList<>();
        for (ClientSession p : players) {
            ids.add(p.getPlayerId());
            scores.put(p.getPlayerId(), 0); // ai cũng bắt đầu với 0 điểm
        }
        engine.spawnPlayers(ids, map.getPlayerSpawnPoints());

        finishRequested = false;
        winnerId = null;
        state = RoomState.PLAYING;

        tickLoop = new TickLoop(tickRate);
        tickLoop.start(this::tick);
    }

    /** Tắt vòng tick. Nếu đang PLAYING thì coi như ván đã FINISHED. */
    public synchronized void stopGame() {
        if (tickLoop != null) {
            tickLoop.stop();
            tickLoop = null;
        }
        if (state == RoomState.PLAYING) {
            state = RoomState.FINISHED;
        }
    }

    /**
     * Thread socket (hoặc ai cũng được) gọi hàm này để xin kết thúc ván.
     * Chỉ bật cờ. Thread tick sẽ tự thấy và kết thúc, vì chỉ thread tick được đọc engine.
     */
    public void requestFinish() {
        finishRequested = true;
    }

    // ================= Điểm số (T21) =================

    /** Cộng điểm cho 1 người. (Sau này M3 báo "A hạ B" thì gọi addKill.) */
    public synchronized void addScore(String playerId, int points) {
        scores.merge(playerId, points, Integer::sum);
    }

    public void addKill(String killerId) {
        addScore(killerId, KILL_POINTS);
    }

    // Trả về BẢN SAO để bên ngoài không sửa trực tiếp vào bảng điểm thật
    public synchronized Map<String, Integer> getScores() {
        return new LinkedHashMap<>(scores);
    }

    public synchronized String getWinnerId() {
        return winnerId;
    }

    // ================= Tick (T15) =================

    /** Thread socket gọi khi nhận lệnh của client. Chỉ bỏ vào hàng đợi, KHÔNG đụng engine. */
    public void submitMove(String playerId, Direction direction) {
        inputQueue.add(new InputCommand(playerId, direction, false));
    }

    /** Thread socket gọi khi client bấm bắn. Cũng chỉ xếp hàng, thread tick mới gọi engine. */
    public void submitShoot(String playerId) {
        inputQueue.add(new InputCommand(playerId, null, true));
    }

    public int getTickCount() {
        return tickCount;
    }

    /** Dành cho test. */
    GameEngine getEngineForTest() {
        return engine;
    }

    /** 1 nhịp tick: (1) lấy lệnh (2) chạy engine (3) kiểm tra hết ván (4) gửi trạng thái. */
    private void tick() {
        if (state != RoomState.PLAYING) {
            return; // ván đã kết thúc, tick còn sót lại thì bỏ qua
        }

        InputCommand cmd;
        while ((cmd = inputQueue.poll()) != null) {
            if (cmd.shoot()) {
                engine.handleShootEvent(cmd.playerId());
            } else {
                engine.handleMoveEvent(cmd.playerId(), cmd.direction());
            }
        }

        engine.update();
        tickCount++;

        if (isGameOver()) {
            finishGame();
            return;
        }

        broadcast(buildBinarySnapshot());
    }

    // Hết ván khi: có người xin kết thúc, hoặc engine báo hết, hoặc chỉ còn tối đa 1 xe sống
    private boolean isGameOver() {
        return finishRequested
                || engine.getGameState().isGameOver()
                || countAliveTanks() <= 1;
    }

    private int countAliveTanks() {
        int alive = 0;
        for (Tank t : engine.getGameState().getTanks()) {
            if (!t.isDestroyed()) {
                alive++;
            }
        }
        return alive;
    }

    /** PLAYING -> FINISHED. Chạy trên thread tick. */
    private void finishGame() {
        GameOverMessage gameOverMsg;
        synchronized (this) {
            if (state != RoomState.PLAYING) {
                return;
            }
            state = RoomState.FINISHED;
            winnerId = findWinner();
            if (tickLoop != null) {
                tickLoop.stop();   // shutdownNow không chờ, nên không bị đứng máy
                tickLoop = null;
            }
            gameOverMsg = new GameOverMessage(winnerId, getScores());
        }
        broadcast(gameOverMsg); // gửi mạng NGOÀI khóa
    }

    // Người thắng = người duy nhất còn xe sống. Nhiều hơn 1 hoặc không còn ai => hòa (null).
    private String findWinner() {
        String found = null;
        for (Tank t : engine.getGameState().getTanks()) {
            if (!t.isDestroyed()) {
                if (found != null) {
                    return null;
                }
                found = t.getId();
            }
        }
        return found;
    }

    public SnapshotMessage buildBinarySnapshot() {
        GameState gameState = engine.getGameState();
        List<TankSnapshot> tankSnapshots = new ArrayList<>();
        for (Tank t : gameState.getTanks()) {
            int starLevel = 0;
            boolean shielded = false;
            if (t instanceof PlayerTank pt) {
                starLevel = pt.getStarLevel();
                shielded = pt.isShielded();
            }
            tankSnapshots.add(new TankSnapshot(
                    t.getId(),
                    t.getPosition().x(),
                    t.getPosition().y(),
                    t.getDirection(),
                    Math.max(1, t.getHealth()),
                    Math.max(1, t.getMaxHealth()),
                    starLevel,
                    shielded,
                    t.isMoving(),
                    t.isDestroyed()
            ));
        }

        List<BulletSnapshot> bulletSnapshots = new ArrayList<>();
        for (Bullet b : gameState.getBullets()) {
            bulletSnapshots.add(new BulletSnapshot(
                    b.getId(),
                    b.getOwnerId(),
                    b.getPosition().x(),
                    b.getPosition().y(),
                    b.getDirection()
            ));
        }

        return new SnapshotMessage(
                tickCount,
                tankSnapshots,
                bulletSnapshots,
                state == RoomState.FINISHED,
                winnerId
        );
    }

    public void broadcast(Message message) {
        List<ClientSession> copy;
        synchronized (this) {
            copy = new ArrayList<>(players);
        }
        for (ClientSession p : copy) {
            try {
                p.send(message);
            } catch (Exception e) {
                System.out.println("[GameRoom] Không gửi được message cho " + p.getPlayerId());
            }
        }
    }
}