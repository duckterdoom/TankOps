package tankops.client.ui.panels;

import tankops.client.ui.MainFrame;
import tankops.client.ui.ScreenState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LobbyPanel extends JPanel {
    public LobbyPanel(MainFrame mainFrame) {
        setBackground(new Color(25, 25, 30));
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Multiplayer Lobby - Room 01", SwingConstants.CENTER);
        title.setFont(new Font("Impact", Font.PLAIN, 26));
        title.setForeground(new Color(240, 190, 40));
        add(title, BorderLayout.NORTH);

        String[] players = {
                " [1] Player1 - READY",
                " [2] TankMaster - READY",
                " [3] Bot_Alpha - READY",
                " [4] Waiting for player...",
                " [5] Waiting for player..."
        };

        JList<String> playerList = new JList<>(players);
        playerList.setFont(new Font("Consolas", Font.BOLD, 15));
        playerList.setBackground(new Color(40, 40, 50));
        playerList.setForeground(Color.GREEN);
        playerList.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), "ROOM PLAYERS (3/5)", 0, 0, null, Color.WHITE
        ));
        add(new JScrollPane(playerList), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btnPanel.setOpaque(false);

        JButton startBtn = new JButton("START");
        startBtn.setFont(new Font("Arial", Font.BOLD, 14));
        startBtn.setBackground(new Color(46, 139, 87));
        startBtn.setForeground(Color.WHITE);

        JButton backBtn = new JButton("LEAVE");
        backBtn.setFont(new Font("Arial", Font.BOLD, 14));
        backBtn.setBackground(new Color(187, 34, 34));
        backBtn.setForeground(Color.WHITE);

        startBtn.addActionListener(e -> mainFrame.showScreen(ScreenState.GAME));
        backBtn.addActionListener(e -> mainFrame.showScreen(ScreenState.MODE_SELECT));

        btnPanel.add(startBtn);
        btnPanel.add(backBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }
}
