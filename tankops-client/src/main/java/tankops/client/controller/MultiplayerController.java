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

        // Vòng lặp 16ms (~60hz) đọc phím bấm và gửi InputMessage
        inputTimer = new Timer(16, e -> {
            if (!networkClient.isConnected() || !gameStateStore.isInGame()) return;

            String playerId = gameStateStore.getAssignedPlayerId();
            if (playerId == null) return;

            int currentTick = gameStateStore.getCurrentTick();
            Direction moveDir = inputHandler.getMovementDirection();
            boolean isShooting = inputHandler.isShooting();

            // Khớp chính xác record InputMessage(playerId, tick, direction, shooting)
            InputMessage inputMsg = new InputMessage(playerId, currentTick, moveDir, isShooting);
            networkClient.sendMessage(inputMsg);
        });

        inputTimer.start();
    }

    /**
     * Dừng vòng lặp gửi Input.
     */
    public void stop() {
        isRunning = false;
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