package tankops.client.ui;

import tankops.client.net.NetworkClient;
import tankops.client.state.GameStateStore;
import tankops.client.ui.panels.*;

import java.awt.*;
import javax.swing.*;

public class MainFrame extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);

    private final GameStateStore gameStateStore;
    private final GamePanel gamePanel;
    private final LobbyPanel lobbyPanel;
    private final RoomPanel roomPanel;
    private final ResultPanel resultPanel;

    public MainFrame() {
        setTitle("TankOps");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        setPreferredSize(new Dimension(1024, 768));
        setMinimumSize(new Dimension(800, 600));

        this.gameStateStore = new GameStateStore();
        NetworkClient.getInstance().addListener(this.gameStateStore);

        this.gamePanel = new GamePanel(this);
        this.lobbyPanel = new LobbyPanel(this, gameStateStore, gamePanel);
        this.roomPanel = new RoomPanel(this, gameStateStore, gamePanel);
        this.resultPanel = new ResultPanel(this);

        mainPanel.add(new LoginPanel(this), ScreenState.LOGIN.name());
        mainPanel.add(new ModeSelectPanel(this), ScreenState.MODE_SELECT.name());
        mainPanel.add(lobbyPanel, ScreenState.LOBBY.name());
        mainPanel.add(roomPanel, ScreenState.ROOM.name());
        mainPanel.add(gamePanel, ScreenState.GAME.name());
        mainPanel.add(resultPanel, ScreenState.RESULTS.name());

        add(mainPanel);
        pack();
        setLocationRelativeTo(null);
        setupGameEventListener();
        showScreen(ScreenState.LOGIN);
    }

    private void setupGameEventListener() {
        this.gameStateStore.addStateChangeListener(() -> {
            if (gameStateStore.takePendingGameOver()) {
                SwingUtilities.invokeLater(() -> {
                    gamePanel.stopGame();
                    resultPanel.updateResults(gameStateStore, true);
                    showScreen(ScreenState.RESULTS);
                });
            }
        });
    }

    public void showScreen(ScreenState state) {
        if (state == ScreenState.GAME) {
            if (gameStateStore.getCurrentRoomId() != null || gameStateStore.isInGame()) {
                gamePanel.startMultiPlayer(gameStateStore);
            } else {
                gamePanel.startSinglePlayer();
            }
        }

        cardLayout.show(mainPanel, state.name());
        mainPanel.requestFocusInWindow();
    }

    public GameStateStore getGameStateStore() {
        return gameStateStore;
    }

    public GamePanel getGamePanel() {
        return gamePanel;
    }

    public RoomPanel getRoomPanel() {
        return roomPanel;
    }

    public ResultPanel getResultPanel() {
        return resultPanel;
    }
}
