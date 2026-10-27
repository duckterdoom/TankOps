package tankops.client.state;

import tankops.client.net.MessageListener;
import tankops.common.net.Message;
import tankops.common.net.game.EnvironmentSnapshot;
import tankops.common.net.game.PowerUpSnapshot;
import tankops.common.net.game.SnapshotMessage;
import tankops.common.net.game.GameOverMessage;
import tankops.common.net.lobby.JoinResponseMessage;
import tankops.common.net.game.EventMessage;
import tankops.common.net.system.ChatMessage;
import tankops.common.net.game.TankSnapshot;
import tankops.common.net.game.BulletSnapshot;
import tankops.common.net.lobby.RoomListResponseMessage;
import tankops.common.net.lobby.RoomInfo;
import tankops.common.net.lobby.RoomStateMessage;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Kho lưu trữ trạng thái game phía Client - thread-safe, Observer Pattern.
 */
public class GameStateStore implements MessageListener {

    private final AtomicReference<SnapshotMessage> latestSnapshot = new AtomicReference<>(null);
    private volatile JoinResponseMessage joinInfo = null;
    private volatile GameOverMessage gameOverState = null;
    private volatile boolean inGame = false;
    private volatile List<RoomInfo> currentRoomList = Collections.emptyList();

    // BỔ SUNG: Danh sách tên tất cả người chơi trong phòng từ Server Broadcast
    private volatile List<String> roomPlayerNames = Collections.emptyList();
    private volatile List<String> readyPlayerNames = Collections.emptyList();

    // [UPDATE]: Biến lưu ID người chơi từ LoginPanel
    private volatile String localPlayerId = null;

    public JoinResponseMessage getJoinInfo() {
        return joinInfo;
    }

    @FunctionalInterface
    public interface StateChangeListener {
        void onStateChanged();
    }

    private final List<StateChangeListener> stateListeners = new CopyOnWriteArrayList<>();

    @Override
    public void onMessageReceived(Message message) {
        switch (message) {
            // XỬ LÝ BROADCAST DANH SÁCH NGƯỜI CHƠI TRONG PHÒNG
            case RoomStateMessage state -> {
                this.roomPlayerNames = state.playerNames();
                this.readyPlayerNames = state.readyPlayerNames(); // Lưu danh sách ready
                notifyStateChanged();
            }
            case RoomListResponseMessage roomList -> {
                this.currentRoomList = roomList.rooms();
                notifyStateChanged();
            }
            case SnapshotMessage snap -> {
                latestSnapshot.set(snap);
                // Khi bắt đầu nhận SnapshotMessage -> tự động đánh dấu đang trong trận
                if (!inGame && !snap.gameOver()) {
                    this.inGame = true;
                }
                notifyStateChanged();
            }
            case GameOverMessage over -> {
                gameOverState = over;
                inGame        = false;
                notifyStateChanged();
            }
            case JoinResponseMessage join -> {
                if (join.success()) {
                    joinInfo = join;
                    inGame   = false;
                    gameOverState = null;
                    latestSnapshot.set(null);
                    // [UPDATE]: Cập nhật ID chính thức do Server cấp nếu có
                    if (join.assignedPlayerId() != null) {
                        this.localPlayerId = join.assignedPlayerId();
                    }
                }
                notifyStateChanged();
            }
            case EventMessage ev -> {
                System.out.println("[GameStateStore] Event: " + ev.eventType()
                                   + " src=" + ev.sourceId() + " tgt=" + ev.targetId());
            }
            case ChatMessage chat -> {
                notifyStateChanged();
            }
            default -> {
            }
        }
    }

    @Override
    public void onConnected(String host, int port) {
        latestSnapshot.set(null);
        joinInfo      = null;
        gameOverState = null;
        inGame        = false;
        roomPlayerNames = Collections.emptyList(); // Reset danh sách khi kết nối lại
        notifyStateChanged();
        System.out.println("[GameStateStore] Đã kết nối tới " + host + ":" + port);
    }

    @Override
    public void onDisconnected(String reason) {
        inGame        = false;
        gameOverState = null;
        roomPlayerNames = Collections.emptyList(); // Reset danh sách khi mất kết nối
        notifyStateChanged();
        System.out.println("[GameStateStore] Mất kết nối: " + reason);
    }

    public SnapshotMessage getLatestSnapshot() {
        return latestSnapshot.get();
    }

    public List<TankSnapshot> getTanks() {
        SnapshotMessage snap = latestSnapshot.get();
        return snap != null ? snap.tanks() : Collections.emptyList();
    }

    public List<BulletSnapshot> getBullets() {
        SnapshotMessage snap = latestSnapshot.get();
        return snap != null ? snap.bullets() : Collections.emptyList();
    }

    /** Vật phẩm đang nằm trên bản đồ (do server gửi trong snapshot). */
    public List<PowerUpSnapshot> getPowerUps() {
        SnapshotMessage snap = latestSnapshot.get();
        return snap != null ? snap.powerUps() : Collections.emptyList();
    }

    /** Tường gạch/thép còn lại (do server gửi trong snapshot). */
    public List<EnvironmentSnapshot> getEnvironments() {
        SnapshotMessage snap = latestSnapshot.get();
        return snap != null ? snap.environments() : Collections.emptyList();
    }

    public int getCurrentTick() {
        SnapshotMessage snap = latestSnapshot.get();
        return snap != null ? snap.tick() : -1;
    }

    public boolean isInGame() {
        return inGame;
    }

    public boolean isGameOver() {
        return gameOverState != null;
    }

    public GameOverMessage getGameOverState() {
        return gameOverState;
    }

    public Map<String, Integer> getScores() {
        GameOverMessage over = gameOverState;
        return over != null ? over.scores() : Collections.emptyMap();
    }

    public String getCurrentRoomId() {
        JoinResponseMessage info = joinInfo;
        return info != null ? info.roomId() : null;
    }

    public String getAssignedPlayerId() {
        JoinResponseMessage info = joinInfo;
        return info != null && info.assignedPlayerId() != null ? info.assignedPlayerId() : localPlayerId;
    }

    // [UPDATE]: Setter cho phép LoginPanel hoặc Client chủ động gán tên người chơi
    public void setAssignedPlayerId(String playerId) {
        this.localPlayerId = playerId;
        notifyStateChanged();
    }

    public void addStateChangeListener(StateChangeListener l) {
        if (l != null) stateListeners.add(l);
    }

    public void removeStateChangeListener(StateChangeListener l) {
        stateListeners.remove(l);
    }

    private void notifyStateChanged() {
        for (StateChangeListener l : stateListeners) {
            try {
                l.onStateChanged();
            } catch (Exception e) {
                System.err.println("[GameStateStore] StateChangeListener lỗi: " + e.getMessage());
            }
        }
    }

    public List<RoomInfo> getCurrentRoomList() {
        return currentRoomList;
    }

    // Lấy danh sách tên người chơi trong phòng từ Broadcast
    public List<String> getRoomPlayerNames() {
        return roomPlayerNames;
    }

    // Kiểm tra xem Client này có phải là Host (Player #1) hay không
    public boolean isHost() {
        JoinResponseMessage info = joinInfo;
        return info != null && info.playerNumber() == 1;
    }

    // Lấy số thứ tự người chơi
    public int getPlayerNumber() {
        JoinResponseMessage info = joinInfo;
        return info != null ? info.playerNumber() : 1;
    }

    public List<String> getReadyPlayerNames() {
        return readyPlayerNames;
    }

    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }
}
