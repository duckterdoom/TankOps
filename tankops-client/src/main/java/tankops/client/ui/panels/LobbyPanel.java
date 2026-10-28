package tankops.client.ui.panels;


import tankops.client.net.NetworkClient;
import tankops.client.state.GameStateStore;
import tankops.client.ui.MainFrame;
import tankops.client.ui.ScreenState;
import tankops.common.net.lobby.JoinRoomMessage;
import tankops.common.net.lobby.LeaveRoomMessage;
import tankops.common.net.lobby.RoomInfo;
import tankops.common.net.lobby.RoomListRequestMessage;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class LobbyPanel extends JPanel {
    private final MainFrame mainFrame;
    private final GameStateStore gameStateStore;
    private final GamePanel gamePanel;


    private final JLabel titleLabel;
    private final JPanel roomsGridPanel;
    private final JButton createBtn;
    private final JButton leaveBtn;


    private Timer refreshTimer;
    private static final int MAX_PLAYERS_PER_ROOM = 5;
    private final List<RoomInfo> roomListData = new ArrayList<>();


    public LobbyPanel(MainFrame mainFrame, GameStateStore gameStateStore, GamePanel gamePanel) {
        this.mainFrame = mainFrame;
        this.gameStateStore = gameStateStore;
        this.gamePanel = gamePanel;


        setBackground(new Color(25, 25, 30));
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));


        // Tiêu đề
        titleLabel = new JLabel("MULTIPLAYER LOBBY", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Impact", Font.PLAIN, 32));
        titleLabel.setForeground(new Color(240, 190, 40));
        add(titleLabel, BorderLayout.NORTH);


        // Panel Lưới 3 cột hình vuông
        roomsGridPanel = new JPanel(new GridLayout(0, 3, 20, 20));
        roomsGridPanel.setOpaque(false);


        JPanel wrapperPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrapperPanel.setOpaque(false);
        wrapperPanel.add(roomsGridPanel);


        JScrollPane scrollPane = new JScrollPane(wrapperPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 80)),
                "AVAILABLE ROOMS (MAX 10)", 0, 0,
                new Font("Arial", Font.BOLD, 12), Color.GRAY
        ));
        add(scrollPane, BorderLayout.CENTER);


        // Panel Nút bấm
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btnPanel.setOpaque(false);


        createBtn = new JButton("CREATE ROOM");
        createBtn.setFont(new Font("Arial", Font.BOLD, 14));
        createBtn.setBackground(new Color(70, 130, 180));
        createBtn.setForeground(Color.WHITE);
        createBtn.setFocusPainted(false);


        leaveBtn = new JButton("LEAVE");
        leaveBtn.setFont(new Font("Arial", Font.BOLD, 14));
        leaveBtn.setBackground(new Color(187, 34, 34));
        leaveBtn.setForeground(Color.WHITE);
        leaveBtn.setFocusPainted(false);


        createBtn.addActionListener(e -> createNewRoom());
        leaveBtn.addActionListener(e -> leaveRoom());


        btnPanel.add(createBtn);
        btnPanel.add(leaveBtn);
        add(btnPanel, BorderLayout.SOUTH);


        initDefaultRooms();
        renderRoomCards();


        if (this.gameStateStore != null) {
            this.gameStateStore.addStateChangeListener(this::onStateChanged);
        }


        // Tự động gửi bản tin xin danh sách phòng lên Server mỗi giây
        startAutoRefresh();

        // Mỗi lần màn hình sảnh được mở lại (về từ phòng, từ màn kết quả...): bật lại việc tự làm mới
        // (timer đã bị tắt khi vào phòng/ván) và xin danh sách phòng ngay để không thấy dữ liệu cũ.
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                if (refreshTimer != null && !refreshTimer.isRunning()) {
                    refreshTimer.start();
                }
                sendRoomListRequest();
            }
        });
    }


    private void startAutoRefresh() {
        refreshTimer = new Timer(1000, e -> {
            if (isShowing()) {
                sendRoomListRequest();
            }
        });
        refreshTimer.start();
    }


    private void sendRoomListRequest() {
        String playerId = gameStateStore != null ? gameStateStore.getAssignedPlayerId() : null;
        if (playerId == null) {
            playerId = "Player_" + System.currentTimeMillis() % 1000;
        }
        // Gửi tin nhắn kèm theo playerId chính xác
        NetworkClient.getInstance().sendMessage(new RoomListRequestMessage(playerId));
    }


    private void initDefaultRooms() {
        roomListData.clear();
        for (int i = 1; i <= 3; i++) {
            roomListData.add(new RoomInfo("room_" + i, 0, MAX_PLAYERS_PER_ROOM, "WAITING"));
        }
    }


    private void renderRoomCards() {
        roomsGridPanel.removeAll();


        for (RoomInfo info : roomListData) {
            JPanel card = createSquareRoomCard(info);
            roomsGridPanel.add(card);
        }


        roomsGridPanel.revalidate();
        roomsGridPanel.repaint();
    }


    private JPanel createSquareRoomCard(RoomInfo info) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout(8, 8));


        // Cố định kích thước 200x200 px
        Dimension squareSize = new Dimension(200, 200);
        card.setPreferredSize(squareSize);
        card.setMinimumSize(squareSize);
        card.setMaximumSize(squareSize);


        card.setBackground(new Color(40, 44, 52));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 85, 100), 2),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));


        JLabel nameLabel = new JLabel(info.roomId(), SwingConstants.CENTER);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        nameLabel.setForeground(Color.WHITE);


        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        infoPanel.setOpaque(false);


        JLabel playersLabel = new JLabel("Players: " + info.currentPlayers() + " / " + info.maxPlayers(), SwingConstants.CENTER);
        playersLabel.setFont(new Font("Consolas", Font.BOLD, 13));
        playersLabel.setForeground(Color.CYAN);


        JLabel statusLabel = new JLabel("Status: " + info.state(), SwingConstants.CENTER);
        statusLabel.setFont(new Font("Consolas", Font.BOLD, 12));
        statusLabel.setForeground("IN_GAME".equalsIgnoreCase(info.state()) ? Color.RED : Color.GREEN);


        infoPanel.add(playersLabel);
        infoPanel.add(statusLabel);


        JButton joinCardBtn = new JButton("JOIN");
        joinCardBtn.setFont(new Font("Arial", Font.BOLD, 13));
        joinCardBtn.setBackground(new Color(46, 139, 87));
        joinCardBtn.setForeground(Color.WHITE);
        joinCardBtn.setFocusPainted(false);
        joinCardBtn.addActionListener(e -> joinRoom(info));


        card.add(nameLabel, BorderLayout.NORTH);
        card.add(infoPanel, BorderLayout.CENTER);
        card.add(joinCardBtn, BorderLayout.SOUTH);


        return card;
    }


    private void onStateChanged() {
        SwingUtilities.invokeLater(() -> {
            if (gameStateStore == null) return;




            // 1. Cập nhật danh sách phòng thực tế từ Server lên UI
            List<RoomInfo> serverRooms = gameStateStore.getCurrentRoomList();
            if (serverRooms != null && !serverRooms.isEmpty()) {
                this.roomListData.clear();
                this.roomListData.addAll(serverRooms);
                renderRoomCards(); // Vẽ lại giao diện danh sách phòng
            }




            // 2. Kiểm tra gói phản hồi JoinResponseMessage gần nhất từ Server
            tankops.common.net.lobby.JoinResponseMessage joinInfo = gameStateStore.getJoinInfo();




            if (joinInfo != null) {
                if (joinInfo.success()) {
                    //THÀNH CÔNG: Server cho phép vào phòng -> Chuyển sang màn hình RoomPanel
                    if (isShowing()) { // Nếu đang ở màn hình LobbyPanel
                        if (refreshTimer != null) refreshTimer.stop();




                        RoomPanel roomPanel = mainFrame.getRoomPanel();
                        if (roomPanel != null) {
                            roomPanel.updateRoomInfo(joinInfo.roomId());
                        }
                        mainFrame.showScreen(ScreenState.ROOM);
                    }
                } else {
                    // THẤT BẠI (Người thứ 6 / Phòng đầy): Hiện Thông Báo Lỗi và Giữ nguyên ở LobbyPanel
                    if (isShowing()) {
                        JOptionPane.showMessageDialog(
                                this,
                                joinInfo.message(), // Nội dung lỗi từ Server (VD: "Phòng đã đầy")
                                "Vào phòng thất bại",
                                JOptionPane.ERROR_MESSAGE
                        );
                        // Reset lại joinInfo trong store để không bị lặp lại Popup ở lần sau
                        gameStateStore.onMessageReceived(new tankops.common.net.lobby.JoinResponseMessage(false, "", null, null, -1));
                    }
                }
            }




            // 3. Nếu trận đấu đã bắt đầu (PLAYING) -> Chuyển sang GamePanel
            if (gameStateStore.isInGame() && !gameStateStore.isGameOver() && isShowing()) {
                if (refreshTimer != null) refreshTimer.stop();
                gamePanel.startMultiPlayer(gameStateStore);
                mainFrame.showScreen(ScreenState.GAME);
            }
        });
    }




    private void joinRoom(RoomInfo info) {
        // 1. Kiểm tra nếu phòng đang trong trận đấu
        if ("IN_GAME".equalsIgnoreCase(info.state()) || "PLAYING".equalsIgnoreCase(info.state())) {
            JOptionPane.showMessageDialog(
                    this,
                    "Phòng '" + info.roomId() + "' đang trong trận đấu! Không thể tham gia lúc này.",
                    "Phòng đang bận",
                    JOptionPane.WARNING_MESSAGE
            );
            return; // Chặn đứng việc gửi request join lên server
        }


        // 2. Kiểm tra nếu phòng đã đầy
        if (info.currentPlayers() >= info.maxPlayers()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Phòng này đã đầy (" + info.currentPlayers() + "/" + info.maxPlayers() + ")!",
                    "Phòng đầy",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }


        String playerName = getPlayerNameForLobby();
        // Gửi yêu cầu vào phòng khi hợp lệ
        NetworkClient.getInstance().sendMessage(new JoinRoomMessage(info.roomId(), playerName));
    }




    private void createNewRoom() {
        String playerName = getPlayerNameForLobby();

        NetworkClient.getInstance().sendMessage(new JoinRoomMessage("new", playerName));
    }


    private void leaveRoom() {
        if (refreshTimer != null) refreshTimer.stop();


        String roomId = gameStateStore.getCurrentRoomId();
        String playerId = gameStateStore.getAssignedPlayerId();


        if (roomId != null && playerId != null) {
            NetworkClient.getInstance().sendMessage(new LeaveRoomMessage(roomId, playerId));
        }


        gamePanel.stopGame();
        mainFrame.showScreen(ScreenState.MODE_SELECT);
    }


    private String getPlayerNameForLobby() {
        String name = tankops.client.state.ClientConfig.getUsername();
        if (name == null || name.isBlank()) {
            if (gameStateStore != null) {
                name = gameStateStore.getAssignedPlayerId();
            }
        }
        if (name == null || name.isBlank()) {
            name = "Player_" + (System.currentTimeMillis() % 1000);
        }
        return name;
    }
}