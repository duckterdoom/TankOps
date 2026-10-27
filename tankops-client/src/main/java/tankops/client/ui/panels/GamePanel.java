package tankops.client.ui.panels;

import tankops.client.controller.MultiplayerController;
import tankops.client.controller.SinglePlayerController;
import tankops.client.state.GameStateStore;
import tankops.client.state.InputHandler;
import tankops.client.ui.MainFrame;
import tankops.client.ui.ScreenState;
import tankops.client.ui.render.GameRenderer;
import tankops.common.engine.GameState;
import tankops.common.model.tank.PlayerTank;
import tankops.common.model.tank.Tank;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GamePanel extends JPanel {
    private final MainFrame mainFrame;
    private final GameRenderer renderer;
    private final InputHandler inputHandler;
    private SinglePlayerController singleController;
    private Timer gameLoopTimer;
    private MultiplayerController multiController;
    private GameStateStore gameStateStore;

    private boolean isMultiplayer = false;

    private int lastKnownScore = 0;
    private int deathCount = 0;
    private int killCount = 0;// Đếm tổng số lần bị bắn chết
    private int previousLives = -1;  // Lưu số mạng ở frame trước để so sánh
    private final java.util.Set<String> activeEnemyIds = new java.util.HashSet<>();

    public GamePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.renderer = new GameRenderer();

        setDoubleBuffered(true);
        setFocusable(true);
        setBackground(new Color(15, 15, 18));

        this.inputHandler = new InputHandler(this);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
            }
        });
    }

    public void startSinglePlayer() {
        stopGame();
        this.isMultiplayer = false;

        // Reset bộ đếm khi bắt đầu ván mới
        this.lastKnownScore = 0;
        this.deathCount = 0;
        this.killCount = 0;
        this.previousLives = -1;
        this.activeEnemyIds.clear();

        tankops.client.sound.SoundManager.getInstance().playSound("stage_start_up.wav");

        this.singleController = new SinglePlayerController();
        this.singleController.initGame();

        setupSinglePlayerLoop();
    }

    public void startMultiPlayer(GameStateStore gameStateStore) {
        stopGame();
        this.isMultiplayer = true;
        this.gameStateStore = gameStateStore;

        this.multiController = new MultiplayerController(inputHandler, gameStateStore);
        this.multiController.start();

        this.gameStateStore.addStateChangeListener(() -> {
            // Kiểm tra nếu Multiplayer kết thúc từ server
            if (gameStateStore.isGameOver()) {
                SwingUtilities.invokeLater(this::handleGameOver);
            } else {
                repaint();
            }
        });
    }

    private void setupSinglePlayerLoop() {
        gameLoopTimer = new Timer(16, e -> {
            if (singleController != null) {
                singleController.handleInput(
                        inputHandler.isUp(),
                        inputHandler.isDown(),
                        inputHandler.isLeft(),
                        inputHandler.isRight(),
                        inputHandler.isShooting()
                );

                singleController.update();

                GameState state = singleController.getGameState();
                if (state != null) {
                    if (state.getTanks() != null) {
                        java.util.Set<String> currentEnemyIds = new java.util.HashSet<>();

                        for (Tank t : state.getTanks()) {
                            if (t instanceof PlayerTank p && t.getId().equals(singleController.getLocalPlayerId())) {
                                // 1. Cập nhật Score
                                lastKnownScore = p.getScore();

                                // 2. Cập nhật Deaths
                                int currentLives = getPlayerLivesReflect(p);
                                if (previousLives != -1 && currentLives < previousLives) {
                                    deathCount += (previousLives - currentLives);
                                }
                                previousLives = currentLives;
                            } else if (!(t instanceof PlayerTank)) {
                                // Thu thập danh sách xe địch đang có mặt
                                currentEnemyIds.add(t.getId());

                                // Nếu xe địch bị đánh dấu isDestroyed trước khi bị xóa
                                if (t.isDestroyed() && activeEnemyIds.contains(t.getId())) {
                                    killCount++;
                                    activeEnemyIds.remove(t.getId());
                                }
                            }
                        }

                        // 🟢 3. ĐẾM KILLS: Nếu xe địch frame trước có mà frame này biến mất (do cleanupDestroyedTanks xóa)
                        for (String prevEnemyId : new java.util.HashSet<>(activeEnemyIds)) {
                            if (!currentEnemyIds.contains(prevEnemyId) && !currentEnemyIds.contains(prevEnemyId)) {
                                killCount++;
                                activeEnemyIds.remove(prevEnemyId);
                            }
                        }
                        activeEnemyIds.addAll(currentEnemyIds);
                    }

                    if (checkSinglePlayerGameOver(state)) {
                        stopGame();
                        handleGameOver();
                        return;
                    }
                }

                repaint();
            }
        });
        gameLoopTimer.start();
    }

    private int getPlayerLivesReflect(PlayerTank player) {
        try {
            var field = PlayerTank.class.getDeclaredField("lives");
            field.setAccessible(true);
            return field.getInt(player);
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * 🟢 Kiểm tra điều kiện kết thúc Single-Player:
     * 1. Engine đánh dấu isGameOver() == true
     * 2. Player 1 bị tiêu diệt hoàn toàn
     * 3. Tiêu diệt hết sạch xe tăng địch (Bots)
     */
    private boolean checkSinglePlayerGameOver(GameState state) {
        if (state == null) return false;

        // 1. Nếu Engine đã bật cờ Game Over
        if (state.isGameOver()) return true;

        if (state.getTanks() == null) return false;

        boolean playerAlive = false;
        boolean enemyAlive = false;

        for (Tank tank : state.getTanks()) {
            if (!tank.isDestroyed()) {
                if (tank instanceof PlayerTank player) {
                    // Kiểm tra nếu PlayerTank còn máu và số mạng > 0 (nếu có getter getLives)
                    // Hoặc đơn giản là kiểm tra sự tồn tại của PlayerTank đang sống
                    playerAlive = true;
                } else {
                    enemyAlive = true;
                }
            }
        }

        // 🔴 CHUYỂN RESULT NGAY KHI:
        // - Player bị tiêu diệt hoàn toàn (!playerAlive)
        // - Hoặc đã dọn sạch toàn bộ Bot địch (!enemyAlive)
        return !playerAlive || !enemyAlive;
    }

    /**
     * 🟢 Chuyển sang ResultPanel và dừng Game
     */
    private void handleGameOver() {
        stopGame();

        ResultPanel resultPanel = mainFrame.getResultPanel();
        if (resultPanel != null) {
            if (this.isMultiplayer) {
                resultPanel.updateResults(this.gameStateStore, true);
            } else {
                GameState state = singleController != null ? singleController.getGameState() : null;
                String playerId = singleController != null ? singleController.getLocalPlayerId() : "PLAYER_1";

                // 🔴 Nếu xe bị nổ hết mạng ở lần chết cuối cùng (khiến PlayerTank bị xóa khỏi GameState)
                // ta cộng nấc chết cuối vào deathCount nếu trước đó xe vẫn còn sống
                boolean playerIsAlive = state != null && state.getTanks() != null &&
                        state.getTanks().stream().anyMatch(t -> t instanceof PlayerTank && !t.isDestroyed());

                int finalDeathCount = deathCount;
                if (!playerIsAlive && (previousLives > 0 || deathCount == 0)) {
                    finalDeathCount++; // Cộng lần chết cuối khiến thua game
                }

                // 🟢 Truyền deathCount và lastKnownScore chuẩn sang ResultPanel
                resultPanel.updateSinglePlayerResults(state, playerId, lastKnownScore, killCount, finalDeathCount);
            }
        }

        mainFrame.showScreen(ScreenState.RESULTS);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        if (isMultiplayer) {
            if (gameStateStore != null && gameStateStore.isInGame()) {
                GameState mapState = singleController != null ? singleController.getGameState() : null;

                renderer.renderSnapshots(
                        g2d,
                        mapState,
                        gameStateStore.getTanks(),
                        gameStateStore.getBullets(),
                        gameStateStore.getAssignedPlayerId(),
                        getWidth(),
                        getHeight()
                );
            }
        } else {
            if (singleController != null) {
                GameState gameState = singleController.getGameState();
                if (gameState != null) {
                    renderer.render(g2d, gameState, singleController.getLocalPlayerId(), getWidth(), getHeight());
                }
            }
        }
    }

    public void stopGame() {
        if (gameLoopTimer != null && gameLoopTimer.isRunning()) {
            gameLoopTimer.stop();
        }
        if (multiController != null) {
            multiController.stop();
        }
    }
}