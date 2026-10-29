package tankops.client.ui.panels;

import tankops.client.controller.MultiplayerController;
import tankops.client.controller.SinglePlayerController;
import tankops.client.state.GameStateStore;
import tankops.client.state.InputHandler;
import tankops.client.ui.MainFrame;
import tankops.client.ui.ScreenState;
import tankops.client.ui.render.GameRenderer;
import tankops.common.engine.GameState;
import tankops.common.map.GameMap;
import tankops.common.map.MapLoader;
import tankops.common.model.common.Vector2D;
import tankops.common.model.environment.BrickWall;
import tankops.common.model.environment.Environment;
import tankops.common.model.environment.SteelWall;
import tankops.common.model.powerups.Grenade;
import tankops.common.model.powerups.Helmet;
import tankops.common.model.powerups.PowerUp;
import tankops.common.model.powerups.Shovel;
import tankops.common.model.powerups.Star;
import tankops.common.model.powerups.TankPowerUp;
import tankops.common.net.game.EnvironmentSnapshot;
import tankops.common.net.game.PowerUpSnapshot;
import tankops.common.net.game.SnapshotMessage;
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

    private GameState multiMapState;
    private GameStateStore.StateChangeListener gameListener;
    private java.util.List<Environment> multiStaticEnvironments = new java.util.ArrayList<>();
    private java.util.List<EnvironmentSnapshot> cachedEnvironments;
    private java.util.List<PowerUpSnapshot> cachedPowerUps;

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
        if (isMultiplayer && this.gameStateStore == gameStateStore
                && gameLoopTimer != null && gameLoopTimer.isRunning()) return;
        stopGame();
        this.isMultiplayer = true;
        this.gameStateStore = gameStateStore;
        this.gameStateStore.setInGame(true);
        this.multiMapState = new GameState();
        cachedEnvironments = null;
        cachedPowerUps = null;
        try {
            MapLoader loader = new MapLoader();
            GameMap multiMap = loader.loadMap("/maps/map_multi.map");
            if (multiMap != null) {
                // Nước, cây, băng không bao giờ đổi trong ván nên client đọc từ file bản đồ.
                // Tường gạch/thép thì server gửi trong snapshot (có thể bị phá hoặc đổi thành thép).
                this.multiStaticEnvironments = new java.util.ArrayList<>();
                for (Environment env : multiMap.getEnvironments()) {
                    if (!(env instanceof BrickWall) && !(env instanceof SteelWall)) {
                        this.multiStaticEnvironments.add(env);
                    }
                }
                this.multiMapState.setEnvironments(this.multiStaticEnvironments);
            }
        } catch (Exception e) {
            System.err.println("[GamePanel] Không thể tải bản đồ /maps/map_multi.map: " + e.getMessage());
        }

        this.multiController = new MultiplayerController(inputHandler, gameStateStore);
        this.multiController.start();

        // Chỉ giữ MỘT listener: bỏ cái của ván trước (trước đây mỗi ván thêm một cái mà không bao giờ gỡ)
        if (gameListener != null) {
            gameStateStore.removeStateChangeListener(gameListener);
        }
        gameListener = () -> {
            // Ván vừa kết thúc từ server: mở màn kết quả ĐÚNG MỘT LẦN
            if (gameStateStore.takePendingGameOver()) {
                SwingUtilities.invokeLater(this::handleGameOver);
            }
        };
        this.gameStateStore.addStateChangeListener(gameListener);
        gameLoopTimer = new Timer(16, e -> repaint());
        gameLoopTimer.start();
        requestFocusInWindow();
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

                        // ĐẾM KILLS: Nếu xe địch frame trước có mà frame này biến mất (do cleanupDestroyedTanks xóa)
                        for (String prevEnemyId : new java.util.HashSet<>(activeEnemyIds)) {
                            if (!currentEnemyIds.contains(prevEnemyId)) {
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
     * Kiểm tra điều kiện kết thúc Single-Player:
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

        // CHUYỂN RESULT NGAY KHI:
        // - Player bị tiêu diệt hoàn toàn (!playerAlive)
        // - Hoặc đã dọn sạch toàn bộ Bot địch (!enemyAlive)
        return !playerAlive || !enemyAlive;
    }

    /**
     * Chuyển sang ResultPanel và dừng Game
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

                //Nếu xe bị nổ hết mạng ở lần chết cuối cùng (khiến PlayerTank bị xóa khỏi GameState)
                // ta cộng nấc chết cuối vào deathCount nếu trước đó xe vẫn còn sống
                boolean playerIsAlive = state != null && state.getTanks() != null &&
                        state.getTanks().stream().anyMatch(t -> t instanceof PlayerTank && !t.isDestroyed());

                int finalDeathCount = deathCount;
                if (!playerIsAlive && (previousLives > 0 || deathCount == 0)) {
                    finalDeathCount++; // Cộng lần chết cuối khiến thua game
                }

                // Truyền deathCount và lastKnownScore chuẩn sang ResultPanel
                resultPanel.updateSinglePlayerResults(state, playerId, lastKnownScore, killCount, finalDeathCount);
            }
        }

        mainFrame.showScreen(ScreenState.RESULTS);
    }

    /**
     * Đưa tường gạch/thép và vật phẩm do SERVER gửi (trong snapshot) vào multiMapState để vẽ.
     * Client không tự đoán tường nào bị phá: server là bên quyết định.
     */
    private void syncMapFromSnapshot(SnapshotMessage snapshot) {
        if (multiMapState == null || gameStateStore == null) {
            return;
        }

        if (!snapshot.environments().equals(cachedEnvironments)) {
            java.util.List<Environment> envs = new java.util.ArrayList<>(multiStaticEnvironments);
            for (EnvironmentSnapshot e : snapshot.environments()) {
                Vector2D pos = new Vector2D(e.x(), e.y());
                if ("SteelWall".equals(e.kind())) {
                    envs.add(new SteelWall(e.id(), pos));
                } else if ("BrickWall".equals(e.kind())) {
                    envs.add(new BrickWall(e.id(), pos));
                }
            }
            multiMapState.setEnvironments(envs);
            cachedEnvironments = snapshot.environments();
        }

        if (!snapshot.powerUps().equals(cachedPowerUps)) {
            multiMapState.getPowerUps().clear();
            for (PowerUpSnapshot p : snapshot.powerUps()) {
                PowerUp powerUp = createPowerUp(p);
                if (powerUp != null) {
                    multiMapState.addPowerUp(powerUp);
                }
            }
            cachedPowerUps = snapshot.powerUps();
        }
    }

    private PowerUp createPowerUp(PowerUpSnapshot p) {
        Vector2D pos = new Vector2D(p.x(), p.y());
        return switch (p.kind()) {
            case "Helmet" -> new Helmet(p.id(), pos);
            case "Grenade" -> new Grenade(p.id(), pos);
            case "Star" -> new Star(p.id(), pos);
            case "TankPowerUp" -> new TankPowerUp(p.id(), pos);
            case "Timer" -> new tankops.common.model.powerups.Timer(p.id(), pos);
            case "Shovel" -> new Shovel(p.id(), pos);
            default -> null; // loại lạ: bỏ qua, không làm vỡ giao diện
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        if (isMultiplayer) {
            if (gameStateStore != null && gameStateStore.isInGame()) {
                SnapshotMessage snapshot = gameStateStore.getRenderSnapshot();
                if (snapshot == null) return;
                syncMapFromSnapshot(snapshot);

                renderer.renderSnapshots(
                        g2d,
                        multiMapState,
                        snapshot.tanks(),
                        snapshot.bullets(),
                        gameStateStore.getActiveExplosions(),
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
        isMultiplayer = false;
        if (gameStateStore != null && gameListener != null) {
            gameStateStore.removeStateChangeListener(gameListener);
            gameListener = null;
        }
        if (gameLoopTimer != null && gameLoopTimer.isRunning()) {
            gameLoopTimer.stop();
        }
        if (multiController != null) {
            multiController.stop();
        }
    }
}
