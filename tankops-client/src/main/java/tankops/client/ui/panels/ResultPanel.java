package tankops.client.ui.panels;

import tankops.client.state.GameStateStore;
import tankops.client.ui.MainFrame;
import tankops.client.ui.ScreenState;
import tankops.common.engine.GameState;
import tankops.common.model.tank.PlayerTank;
import tankops.common.model.tank.Tank;
import tankops.common.net.game.GameOverMessage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.Map;

public class ResultPanel extends JPanel {
    private final JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 10));
    private final MainFrame mainFrame;
    private final DefaultTableModel tableModel;
    private final JLabel titleLabel;
    private final JLabel subTitleLabel;

    public ResultPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setBackground(new Color(25, 25, 30));
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        headerPanel.setOpaque(false);

        titleLabel = new JLabel("MATCH FINISHED", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Impact", Font.PLAIN, 42));
        titleLabel.setForeground(new Color(240, 190, 40));

        subTitleLabel = new JLabel("RESULT & STATUS", SwingConstants.CENTER);
        subTitleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        subTitleLabel.setForeground(Color.LIGHT_GRAY);

        headerPanel.add(titleLabel);
        headerPanel.add(subTitleLabel);
        add(headerPanel, BorderLayout.NORTH);

        String[] columnNames = {"RANK", "PLAYER", "KILLS", "DEATHS", "SCORE", "STATUS"};

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        styleResultTable(table);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 100)));
        add(scrollPane, BorderLayout.CENTER);

        actionPanel.setOpaque(false);
        add(actionPanel, BorderLayout.SOUTH);

        setupButtons(false);
    }

    /**
     * 🟢 CẬP NHẬT THÔNG TIN CHO SINGLE-PLAYER
     */
    /**
     * 🟢 CẬP NHẬT THÔNG TIN CHO SINGLE-PLAYER (Đã sửa lỗi Deaths & Score)
     */
    public void updateSinglePlayerResults(GameState gameState, String localPlayerId, int lastKnownScore, int killCount, int deathCount) {
        setupButtons(false);
        tableModel.setRowCount(0);

        if (gameState == null) {
            titleLabel.setText("GAME OVER");
            titleLabel.setForeground(new Color(240, 190, 40));
            tableModel.addRow(new Object[]{"#1", "PLAYER 1", 0, 0, 0, "FINISHED"});
            return;
        }

        PlayerTank player = null;

        // Duyệt danh sách xe tăng trong GameState
        if (gameState.getTanks() != null) {
            for (Tank tank : gameState.getTanks()) {
                if (tank instanceof PlayerTank p && tank.getId().equals(localPlayerId)) {
                    player = p;
                    break;
                }
            }
        }

        int score = lastKnownScore;
        boolean isVictory = player != null && !player.isDestroyed();

        if (isVictory) {
            titleLabel.setText("VICTORY!");
            titleLabel.setForeground(new Color(50, 205, 50));
        } else {
            titleLabel.setText("DEFEAT");
            titleLabel.setForeground(new Color(220, 50, 50));
        }

        String status = isVictory ? "VICTORIOUS" : "DESTROYED";

        tableModel.addRow(new Object[]{
                "#1",
                "PLAYER 1 (YOU)",
                killCount,
                deathCount,
                score,
                status
        });
    }

    // Helper lấy số mạng của PlayerTank bằng reflection
    private int getPlayerLives(PlayerTank player) {
        try {
            var method = PlayerTank.class.getMethod("getLives");
            return (int) method.invoke(player);
        } catch (Exception e1) {
            try {
                var field = PlayerTank.class.getDeclaredField("lives");
                field.setAccessible(true);
                return field.getInt(player);
            } catch (Exception e2) {
                return player.isDestroyed() ? 0 : 1;
            }
        }
    }

    public void updateResults(GameStateStore gameStateStore, boolean isMultiplayer) {
        setupButtons(isMultiplayer);
        tableModel.setRowCount(0);

        if (isMultiplayer && gameStateStore != null) {
            GameOverMessage gameOver = gameStateStore.getGameOverState();
            String localPlayerId = gameStateStore.getAssignedPlayerId();

            if (gameOver != null && gameOver.scores() != null && !gameOver.scores().isEmpty()) {
                Map<String, Integer> scores = gameOver.scores();
                String winnerId = gameOver.winnerId();

                boolean isVictory = (winnerId != null && localPlayerId != null && winnerId.equalsIgnoreCase(localPlayerId));
                if (isVictory) {
                    titleLabel.setText("VICTORY!");
                    titleLabel.setForeground(new Color(50, 205, 50));
                } else {
                    titleLabel.setText("DEFEAT");
                    titleLabel.setForeground(new Color(220, 50, 50));
                }

                java.util.List<Map.Entry<String, Integer>> sortedList = new java.util.ArrayList<>();
                Map.Entry<String, Integer> winnerEntry = null;

                for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                    if (winnerId != null && entry.getKey().equalsIgnoreCase(winnerId)) {
                        winnerEntry = entry;
                    } else {
                        sortedList.add(entry);
                    }
                }

                // Sắp xếp danh sách người thua theo Score giảm dần
                sortedList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

                // Đưa Người thắng (Winner) lên vị trí đầu tiên (Index 0) nếu có
                if (winnerEntry != null) {
                    sortedList.add(0, winnerEntry);
                }

                //ĐƯA DỮ LIỆU ĐÃ SẮP XẾP LÊN BẢNG RESULT
                int rank = 1;
                for (Map.Entry<String, Integer> entry : sortedList) {
                    String pId = entry.getKey();
                    int score = entry.getValue();

                    boolean isWinner = (winnerId != null && winnerId.equalsIgnoreCase(pId));
                    String status = isWinner ? "MVP / WINNER" : "DEFEATED";

                    boolean isMe = (localPlayerId != null && localPlayerId.equalsIgnoreCase(pId));

                    String displayName = pId;
                    if (isMe) {
                        displayName += " (YOU)";
                    }

                    int estimatedKills = score / 100;
                    String killsText = String.valueOf(estimatedKills);
                    String deathsText = isWinner ? "0" : "1";

                    tableModel.addRow(new Object[]{
                            "#" + rank++,
                            displayName,
                            killsText,
                            deathsText,
                            score,
                            status
                    });
                }
            } else {
                tableModel.addRow(new Object[]{"#1", "Waiting for server...", 0, 0, 0, "FINISHED"});
            }
        } else {
            titleLabel.setText("GAME OVER");
            titleLabel.setForeground(new Color(240, 190, 40));
            tableModel.addRow(new Object[]{"#1", "Player 1", 5, 0, 1000, "COMPLETED"});
        }
    }

    private void setupButtons(boolean isMultiplayer) {
        actionPanel.removeAll();

        if (isMultiplayer) {
            JButton lobbyBtn = createStyleButton("BACK TO LOBBY", new Color(70, 130, 180));
            JButton menuBtn = createStyleButton("MAIN MENU", new Color(178, 34, 34));
            lobbyBtn.addActionListener(e -> mainFrame.showScreen(ScreenState.LOBBY));
            menuBtn.addActionListener(e -> mainFrame.showScreen(ScreenState.MODE_SELECT));

            actionPanel.add(lobbyBtn);
            actionPanel.add(menuBtn);
        } else {
            JButton playAgainBtn = createStyleButton("PLAY AGAIN", new Color(46, 139, 87));
            JButton menuBtn = createStyleButton("MAIN MENU", new Color(178, 34, 34));
            playAgainBtn.addActionListener(e -> mainFrame.showScreen(ScreenState.GAME));
            menuBtn.addActionListener(e -> mainFrame.showScreen(ScreenState.MODE_SELECT));

            actionPanel.add(playAgainBtn);
            actionPanel.add(menuBtn);
        }

        actionPanel.revalidate();
        actionPanel.repaint();
    }

    private void styleResultTable(JTable table) {
        table.setFont(new Font("Consolas", Font.BOLD, 14));
        table.setRowHeight(35);
        table.setShowGrid(true);
        table.setGridColor(new Color(50, 50, 65));
        table.setBackground(new Color(35, 35, 45));
        table.setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(60, 60, 80));

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 14));
        header.setBackground(new Color(45, 45, 60));
        header.setForeground(new Color(240, 190, 40));
        header.setPreferredSize(new Dimension(100, 40));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    private JButton createStyleButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(160, 40));
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        return button;
    }
}