package tankops.client.ui.panels;


import tankops.client.net.NetworkClient;
import tankops.client.state.GameStateStore;
import tankops.client.ui.MainFrame;
import tankops.client.ui.ScreenState;
import tankops.common.net.lobby.LeaveRoomMessage;
import tankops.common.net.system.ChatMessage;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;


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
    private boolean isHost = false;


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


        // Danh sách người chơi
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


        // Panel nút hành động
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


        String currentRoomId = gameStateStore.getCurrentRoomId();
        if (currentRoomId != null) {
            roomTitleLabel.setText("ROOM: " + currentRoomId.toUpperCase());
        }


        this.isHost = gameStateStore.isHost();


        List<String> playerNames = gameStateStore.getRoomPlayerNames();
        List<String> readyNames = gameStateStore.getReadyPlayerNames();
        String myName = gameStateStore.getAssignedPlayerId();


        boolean allMembersReady = checkAllMembersReady(playerNames, readyNames);


        if (playerNames != null) {
            for (int i = 0; i < playerNames.size(); i++) {
                String pName = playerNames.get(i);
                boolean isRoomHost = (i == 0);


                String roleText = isRoomHost ? " (HOST)" : " (MEMBER)";
                String selfBadge = pName.equals(myName) ? " [YOU]" : "";
                String readyText = "";


                if (!isRoomHost) {
                    boolean isReadyPlayer = readyNames != null && readyNames.contains(pName);
                    readyText = isReadyPlayer ? " - [READY]" : " - [NOT READY]";
                }


                playerListModel.addElement(String.format("P%d: %s%s%s%s", (i + 1), pName, roleText, readyText, selfBadge));
            }
        }


        int totalPlayers = playerNames != null ? playerNames.size() : 1;
        boolean canStart = isHost && (totalPlayers >= 2) && allMembersReady;


        startBtn.setVisible(isHost);

        startBtn.setToolTipText(getStartButtonTooltip(isHost, totalPlayers, allMembersReady));

        readyBtn.setVisible(!isHost);


        revalidate();
        repaint();
    }

    private boolean checkAllMembersReady(List<String> playerNames, List<String> readyNames) {
        if (playerNames == null || playerNames.size() < 2) return false;
        // Kiểm tra từ vị trí index 1 trở đi (bỏ qua Host ở index 0)
        for (int i = 1; i < playerNames.size(); i++) {
            if (readyNames == null || !readyNames.contains(playerNames.get(i))) {
                return false;
            }
        }
        return true;
    }


    private String getStartButtonTooltip(boolean isHost, int totalPlayers, boolean allReady) {
        if (!isHost) return null;
        if (totalPlayers < 2) return "Cần tối thiểu 2 người chơi để bắt đầu!";
        if (!allReady) return "Đang chờ tất cả thành viên bấm READY!";
        return null;
    }
    private void toggleReady() {
        isReady = !isReady;
        readyBtn.setText(isReady ? "UNREADY" : "READY");
        readyBtn.setBackground(isReady ? new Color(218, 165, 32) : new Color(46, 139, 87));


        String roomId = gameStateStore.getCurrentRoomId();
        String playerId = gameStateStore.getAssignedPlayerId();
        if (roomId != null && playerId != null) {
            NetworkClient.getInstance().sendMessage(
                    new ChatMessage(playerId, isReady ? "READY" : "UNREADY")
            );
        }
    }


    private void startGame() {
        if (gameStateStore == null || !gameStateStore.isHost()) return;


        List<String> playerNames = gameStateStore.getRoomPlayerNames();
        List<String> readyNames = gameStateStore.getReadyPlayerNames();
        int totalPlayers = playerNames != null ? playerNames.size() : 1;


        // 1. Chưa đủ người chơi -> Bật popup thông báo
        if (totalPlayers < 2) {
            JOptionPane.showMessageDialog(
                    this,
                    "Cần tối thiểu 2 người chơi trong phòng để bắt đầu trận đấu!",
                    "Chưa đủ người",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }


        // 2. Còn thành viên chưa Ready -> Bật popup thông báo
        if (!checkAllMembersReady(playerNames, readyNames)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không thể bắt đầu! Vẫn còn thành viên trong phòng chưa bấm READY.",
                    "Chưa sẵn sàng",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }


        // 3. Đã đủ điều kiện -> Gửi lệnh bắt đầu trận đấu lên Server
        NetworkClient.getInstance().sendMessage(
                new ChatMessage(gameStateStore.getAssignedPlayerId(), "/start")
        );
    }


    private void leaveRoom() {
        String roomId = gameStateStore != null ? gameStateStore.getCurrentRoomId() : null;
        String playerId = gameStateStore != null ? gameStateStore.getAssignedPlayerId() : null;

        isReady = false;
        if (roomId != null && playerId != null) {
            NetworkClient.getInstance().sendMessage(new LeaveRoomMessage(roomId, playerId));
        }

        mainFrame.showScreen(ScreenState.LOBBY);
    }

    private void onStateChanged() {
        SwingUtilities.invokeLater(() -> {
            if (gameStateStore != null && gameStateStore.getCurrentRoomId() != null) {
                refreshPlayerList();
            }
            if (gameStateStore != null && gameStateStore.isInGame() && !gameStateStore.isGameOver() && isShowing()) {
                gamePanel.startMultiPlayer(gameStateStore);
                mainFrame.showScreen(ScreenState.GAME);
            }
        });
    }

    private void showWarning(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
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