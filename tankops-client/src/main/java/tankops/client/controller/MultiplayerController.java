package tankops.client.controller;

import tankops.client.net.NetworkClient;
import tankops.client.state.GameStateStore;
import tankops.client.state.InputHandler;
import tankops.common.model.common.Direction;
import tankops.common.net.game.InputMessage;
import tankops.common.net.lobby.JoinRoomMessage;

import javax.swing.Timer;

public class MultiplayerController {

    private final NetworkClient networkClient;
    private final InputHandler inputHandler;
    private final GameStateStore gameStateStore;

    private Timer inputTimer;
    private boolean isRunning = false;
    private Direction lastDirection;
    private boolean lastShooting;
    private long lastSentAt;

    public MultiplayerController(InputHandler inputHandler, GameStateStore gameStateStore) {
        this.inputHandler = inputHandler;
        this.gameStateStore = gameStateStore;
        this.networkClient = NetworkClient.getInstance();
    }

    /**
     * Bắt đầu vòng lặp gửi Input lên Server (~60 FPS).
     */
    public void start() {
        if (isRunning) return;
        isRunning = true;

        lastSentAt = 0;
        inputHandler.setOnInputChanged(() -> sendInput(false));
        // Changes are sent immediately; held fire is sampled at the server's default 30 Hz.
        inputTimer = new Timer(33, e -> sendInput(true));
        inputTimer.start();
        sendInput(false);
    }

    private void sendInput(boolean pulse) {
        if (!isRunning || !networkClient.isConnected() || !gameStateStore.isInGame()) return;
        networkClient.requestLatencySample();
        String playerId = gameStateStore.getAssignedPlayerId();
        if (playerId == null) return;
        Direction direction = inputHandler.getMovementDirection();
        boolean shooting = inputHandler.isShooting();
        long now = System.nanoTime();
        boolean changed = direction != lastDirection || shooting != lastShooting;
        if (!changed && lastSentAt != 0 && !(pulse && shooting)
                && now - lastSentAt < 100_000_000L) return;
        if (networkClient.sendMessage(new InputMessage(playerId, gameStateStore.getCurrentTick(), direction, shooting))) {
            lastDirection = direction;
            lastShooting = shooting;
            lastSentAt = now;
        }
    }

    /**
     * Dừng vòng lặp gửi Input.
     */
    public void stop() {
        isRunning = false;
        inputHandler.setOnInputChanged(null);
        if (inputTimer != null) {
            inputTimer.stop();
        }
    }

    public void requestCreateRoom() {
        if (networkClient != null && networkClient.isConnected()) {
            String playerName = gameStateStore.getAssignedPlayerId();
            if (playerName == null || playerName.isBlank()) {
                playerName = "Player_" + (System.currentTimeMillis() % 1000);
            }

            // Truyền "new" chuẩn xác theo RoomManager.NEW_ROOM
            JoinRoomMessage createMsg = new JoinRoomMessage("new", playerName);
            networkClient.sendMessage(createMsg);
        }
    }
}
