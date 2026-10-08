package tankops.client.ui.panels;

import tankops.client.ui.MainFrame;
import tankops.client.ui.ScreenState;

import javax.swing.*;
import java.awt.*;

public class LobbyPanel extends JPanel {
    public LobbyPanel(MainFrame mainFrame) {
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("Game Lobby - Room 01", SwingConstants.CENTER);
        title.setFont(new Font("arial", Font.BOLD, 20));
        add(title, BorderLayout.NORTH);

        String[] players = {"1. Player1 (You) - READY", "2. TankMaster - READY", "3. Bot_Alpha - READY"};

        JList<String> playerList = new JList<>(players);
        add(new JScrollPane(playerList), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton startBtn = new JButton("Start match");
        JButton backBtn = new JButton("Disconnect");

        startBtn.addActionListener(e -> mainFrame.showScreen(ScreenState.GAME));
        backBtn.addActionListener(e -> mainFrame.showScreen(ScreenState.LOGIN));

        btnPanel.add(startBtn);
        btnPanel.add(backBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }
}
