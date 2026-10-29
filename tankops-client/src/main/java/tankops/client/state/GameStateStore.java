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
    private final RenderInterpolator renderInterpolator = new RenderInterpolator();

    public SnapshotMessage getRenderSnapshot() {
        return renderInterpolator.sample(getAssignedPlayerId());
    }
    private volatile JoinResponseMessage joinInfo = null;
    private final AtomicReference<String> joinError = new AtomicReference<>();
    private volatile GameOverMessage gameOverState = null;
    // true từ lúc nhận GameOverMessage cho tới khi màn hình kết quả đã được mở (chỉ mở đúng 1 lần mỗi ván)
    private final java.util.concurrent.atomic.AtomicBoolean gameOverPending = new java.util.concurrent.atomic.AtomicBoolean(false);
    private volatile boolean inGame = false;
    private volatile List<RoomInfo> currentRoomList = Collections.emptyList();

    // BỔ SUNG: Danh sách tên tất cả người chơi trong phòng từ Server Broadcast
    private volatile List<String> roomPlayerNames = Collections.emptyList();
    private volatile List<String> readyPlayerNames = Collections.emptyList();

    // [UPDATE]: Biến lưu ID người chơi từ LoginPanel
    private volatile String localPlayerId = null;

    public static record ExplosionEffect(double x, double y, boolean isBig, long spawnTime) {}

    private final List<ExplosionEffect> activeExplosions = new CopyOnWriteArrayList<>();
    private volatile List<TankSnapshot> previousTanks = Collections.emptyList();
    private volatile List<BulletSnapshot> previousBullets = Collections.emptyList();

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
                renderInterpolator.push(snap);
                processExplosionDetection(snap.tanks(), snap.bullets());
                // Phát âm thanh server gửi kèm snapshot (mỗi tick chỉ phát 1 lần)
                for (String sound : snap.sounds()) {
                    tankops.client.sound.SoundManager.getInstance().playSound(sound);
                }
                // Khi bắt đầu nhận SnapshotMessage -> tự động đánh dấu đang trong trận
                if (!inGame && !snap.gameOver()) {
                    this.inGame = true;
                    tankops.client.sound.SoundManager.getInstance().playSound("stage_start_up.wav");
                }
                notifyStateChanged();
            }
            case GameOverMessage over -> {
                gameOverState = over;
                tankops.client.sound.SoundManager.getInstance().playSound("game_over.wav");
                gameOverPending.set(true);
                inGame        = false;
                notifyStateChanged();
            }
            case JoinResponseMessage join -> {
                if (join.success()) {
                    renderInterpolator.clear();
                    joinInfo = join;
                    inGame   = false;
                    gameOverState = null;
                    gameOverPending.set(false);
                    latestSnapshot.set(null);
                    // [UPDATE]: Cập nhật ID chính thức do Server cấp nếu có
                    if (join.assignedPlayerId() != null) {
                        this.localPlayerId = join.assignedPlayerId();
                    }
                } else {
                    joinError.set(join.message());
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
        renderInterpolator.clear();
        latestSnapshot.set(null);
        joinInfo      = null;
        gameOverState = null;
        inGame        = false;
        roomPlayerNames = Collections.emptyList(); // Reset danh sách khi kết nối lại
        readyPlayerNames = Collections.emptyList();
        joinError.set(null);
        notifyStateChanged();
        System.out.println("[GameStateStore] Đã kết nối tới " + host + ":" + port);
    }

    @Override
    public void onDisconnected(String reason) {
        currentRoomList = Collections.emptyList();
        clearRoom();
        System.out.println("[GameStateStore] Mất kết nối: " + reason);
    }

    public SnapshotMessage getLatestSnapshot() {
        return latestSnapshot.get();
    }

    public String consumeJoinError() {
        return joinError.getAndSet(null);
    }

    public void clearRoom() {
        renderInterpolator.clear();
        joinInfo = null;
        joinError.set(null);
        inGame = false;
        gameOverState = null;
        latestSnapshot.set(null);
        roomPlayerNames = Collections.emptyList();
        readyPlayerNames = Collections.emptyList();
        notifyStateChanged();
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

    /**
     * Trả về true ĐÚNG MỘT LẦN sau mỗi GameOverMessage. Các nơi muốn mở màn hình kết quả phải hỏi hàm này,
     * để không bị mở lại mỗi lần danh sách phòng cập nhật (lỗi cũ: bấm vào lại phòng là nhảy về màn kết quả).
     */
    public boolean takePendingGameOver() {
        return gameOverPending.getAndSet(false);
    }

    /**
     * Gọi khi rời màn hình kết quả (về lobby/menu): xóa trạng thái của ván cũ và của phòng cũ.
     */
    public void resetAfterGame() {
        renderInterpolator.clear();
        gameOverState = null;
        gameOverPending.set(false);
        inGame = false;
        latestSnapshot.set(null);
        joinInfo = null;
        roomPlayerNames = Collections.emptyList();
        readyPlayerNames = Collections.emptyList();
        notifyStateChanged();
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
        // Chủ phòng là người đứng đầu danh sách do server gửi (đổi người khi chủ phòng rời đi)
        List<String> names = roomPlayerNames;
        String me = getAssignedPlayerId();
        if (names != null && !names.isEmpty() && me != null) {
            return names.get(0).equals(me);
        }
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

    public List<ExplosionEffect> getActiveExplosions() {
        long now = System.currentTimeMillis();
        // Xóa các hiệu ứng nổ kéo dài quá 250ms
        activeExplosions.removeIf(e -> now - e.spawnTime() > 250);
        return activeExplosions;
    }

    private void processExplosionDetection(List<TankSnapshot> newTanks, List<BulletSnapshot> newBullets) {
        long now = System.currentTimeMillis();

        // 1. PHÁT HIỆN ĐẠN NỔ (Đạn frame trước có mà frame này biến mất)
        if (previousBullets != null && !previousBullets.isEmpty()) {
            java.util.Set<String> newBulletIds = new java.util.HashSet<>();
            if (newBullets != null) {
                for (BulletSnapshot b : newBullets) {
                    newBulletIds.add(b.id());
                }
            }

            for (BulletSnapshot oldB : previousBullets) {
                if (!newBulletIds.contains(oldB.id())) {
                    activeExplosions.add(new ExplosionEffect(oldB.x(), oldB.y(), false, now));
                }
            }
        }

        // 2. PHÁT HIỆN XE NỔ (Xe frame trước còn sống mà frame này bị destroyed/biến mất)
        if (previousTanks != null && !previousTanks.isEmpty()) {
            java.util.Map<String, TankSnapshot> newTankMap = new java.util.HashMap<>();
            if (newTanks != null) {
                for (TankSnapshot t : newTanks) {
                    newTankMap.put(t.id(), t);
                }
            }

            for (TankSnapshot oldT : previousTanks) {
                TankSnapshot newT = newTankMap.get(oldT.id());
                if (!oldT.destroyed() && (newT == null || newT.destroyed())) {
                    activeExplosions.add(new ExplosionEffect(oldT.x() + 13, oldT.y() + 13, true, now));
                }
            }
        }

        this.previousTanks = newTanks != null ? new java.util.ArrayList<>(newTanks) : Collections.emptyList();
        this.previousBullets = newBullets != null ? new java.util.ArrayList<>(newBullets) : Collections.emptyList();
    }
}
