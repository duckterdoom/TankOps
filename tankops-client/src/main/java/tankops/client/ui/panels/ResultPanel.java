package tankops.client.ui.panels;

import tankops.client.ui.MainFrame;
import tankops.client.ui.ScreenState;

import javax.swing.*;
import java.awt.*;

public class ResultPanel extends JPanel {
    public ResultPanel(MainFrame mainFrame) {
        setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("Match Results", SwingConstants.CENTER);
        title.setFont(new Font("arial", Font.BOLD, 22));
        add(title, BorderLayout.NORTH);

        String[] results = {"1st Place: Player1 - 5 Kills / 0 Deaths", "2nd Place: TankMaster - 2 Kills / 3 Deaths"};
        JList<String> resultList = new JList<>(results);
        add(new JScrollPane(resultList), BorderLayout.CENTER);

        JButton backBtn = new JButton("Back to Lobby");
        backBtn.addActionListener(e -> mainFrame.showScreen(ScreenState.LOBBY));
        add(backBtn, BorderLayout.SOUTH);
    }
}
