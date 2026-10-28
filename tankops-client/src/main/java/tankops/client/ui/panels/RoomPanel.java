package tankops.client.ui.panels;

import tankops.client.net.NetworkClient;
import tankops.client.state.GameStateStore;
import tankops.client.ui.MainFrame;
import tankops.client.ui.ScreenState;
import tankops.common.net.lobby.LeaveRoomMessage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class RoomPanel extends JPanel {
    private final MainFrame mainFrame;
    private final GameStateStore gameStateStore;
    private final GamePanel gamePanel;

    private final JLabel roomTitleLabel;
    private final DefaultListModel<String> playerListModel;
    private final JButton readyBtn;
    private final JButton startBtn;
    private final JButton leaveBtn;

    private boolean isReady = false;
    private boolean isHost = false; // Có thể cập nhật dựa vào tin nhắn từ Server

    public RoomPanel(MainFrame mainFrame, GameStateStore gameStateStore, GamePanel gamePanel) {
        this.mainFrame = mainFrame;
        this.gameStateStore = gameStateStore;
        this.gamePanel = gamePanel;

        setBackground(new Color(25, 25, 30));
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(30, 40, 30, 40));

        // Header tiêu đề phòng
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        headerPanel.setOpaque(false);

        roomTitleLabel = new JLabel("WAITING ROOM", SwingConstants.CENTER);
        roomTitleLabel.setFont(new Font("Impact", Font.PLAIN, 36));
        roomTitleLabel.setForeground(new Color(240, 190, 40));

        JLabel subTitleLabel = new JLabel("WAITING FOR PLAYERS...", SwingConstants.CENTER);
        subTitleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        subTitleLabel.setForeground(Color.LIGHT_GRAY);

        headerPanel.add(roomTitleLabel);
        headerPanel.add(subTitleLabel);
        add(headerPanel, BorderLayout.NORTH);

        // Danh sách người chơi trong phòng
        playerListModel = new DefaultListModel<>();
        JList<String> playerList = new JList<>(playerListModel);
        stylePlayerList(playerList);

        JScrollPane scrollPane = new JScrollPane(playerList);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 100)),
                "PLAYERS IN ROOM", 0, 0,
                new Font("Arial", Font.BOLD, 13), new Color(240, 190, 40)
        ));
        add(scrollPane, BorderLayout.CENTER);

        // Panel nút hành động ở dưới
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 10));
        actionPanel.setOpaque(false);

        readyBtn = createStyleButton("READY", new Color(46, 139, 87));
        startBtn = createStyleButton("START GAME", new Color(70, 130, 180));
        leaveBtn = createStyleButton("LEAVE ROOM", new Color(178, 34, 34));

        readyBtn.addActionListener(e -> toggleReady());
        startBtn.addActionListener(e -> startGame());
        leaveBtn.addActionListener(e -> leaveRoom());

        actionPanel.add(readyBtn);
        actionPanel.add(startBtn);
        actionPanel.add(leaveBtn);
        add(actionPanel, BorderLayout.SOUTH);

        // Lắng nghe trạng thái khi bắt đầu game
        if (this.gameStateStore != null) {
            this.gameStateStore.addStateChangeListener(this::onStateChanged);
        }
    }

    public void updateRoomInfo(String roomId) {
        if (roomId != null) {
            roomTitleLabel.setText("ROOM: " + roomId.toUpperCase());
        }
        refreshPlayerList();
    }

    public void refreshPlayerList() {
        playerListModel.clear();

        if (gameStateStore == null) return;

        String localPlayerId = gameStateStore.getAssignedPlayerId();
        String currentRoomId = gameStateStore.getCurrentRoomId();

        // 🟢 Đọc đúng vai trò dựa vào playerNumber từ JoinResponseMessage
        this.isHost = gameStateStore.isHost();
        int playerNum = gameStateStore.getPlayerNumber();

        if (currentRoomId != null) {
            roomTitleLabel.setText("ROOM: " + currentRoomId.toUpperCase());
        }

        if (localPlayerId != null) {
            String roleText = isHost ? " (HOST)" : " (MEMBER)";
            String playerSlot = "P" + playerNum + ": ";
            String readyText = isReady ? " - READY" : " - NOT READY";

            playerListModel.addElement((isHost ? "👑 " : "👤 ") + playerSlot + localPlayerId + roleText + readyText);
        }

        // 🟢 Nút START GAME chỉ hiện cho Host (Player #1), Nút READY hiện cho Member
        startBtn.setVisible(isHost);
        readyBtn.setVisible(!isHost);

        revalidate();
        repaint();
    }

    private void toggleReady() {
        isReady = !isReady;
        if (isReady) {
            readyBtn.setText("UNREADY");
            readyBtn.setBackground(new Color(218, 165, 32));
        } else {
            readyBtn.setText("READY");
            readyBtn.setBackground(new Color(46, 139, 87));
        }
        // Gửi thông điệp Ready/Unready lên server ở đây
    }

    private void startGame() {
        if (gameStateStore != null) {
            gameStateStore.setInGame(true);
            gamePanel.startMultiPlayer(gameStateStore);
            mainFrame.showScreen(ScreenState.GAME);
        }
    }

    private void leaveRoom() {
        String roomId = gameStateStore != null ? gameStateStore.getCurrentRoomId() : null;
        String playerId = gameStateStore != null ? gameStateStore.getAssignedPlayerId() : null;

        if (roomId != null && playerId != null) {
            NetworkClient.getInstance().sendMessage(new LeaveRoomMessage(roomId, playerId));
        }

        mainFrame.showScreen(ScreenState.LOBBY);
    }

    private void onStateChanged() {
        SwingUtilities.invokeLater(() -> {
            // Khi Server trả về JoinResponseMessage thành công
            if (gameStateStore != null && gameStateStore.getCurrentRoomId() != null) {
                refreshPlayerList();
            }

            // Khi Server phát lệnh bắt đầu trận đấu (isInGame == true)
            if (gameStateStore != null && gameStateStore.isInGame() && !gameStateStore.isGameOver() && isShowing()) {
                gamePanel.startMultiPlayer(gameStateStore);
                mainFrame.showScreen(ScreenState.GAME);
            }
        });
    }

    private void stylePlayerList(JList<String> list) {
        list.setFont(new Font("Consolas", Font.BOLD, 16));
        list.setBackground(new Color(35, 35, 45));
        list.setForeground(Color.WHITE);
        list.setSelectionBackground(new Color(60, 60, 80));
        list.setFixedCellHeight(40);
    }

    private JButton createStyleButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(150, 40));
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        return button;
    }
}