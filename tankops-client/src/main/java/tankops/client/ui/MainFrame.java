package tankops.client.ui;

import tankops.client.ui.panels.*;

import java.awt.*;
import javax.swing.*;

public class MainFrame extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);

    public MainFrame() {
        setTitle("TankOps");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        setPreferredSize(new Dimension(1024, 768));
        setMinimumSize(new Dimension(800, 600));

        mainPanel.add(new LoginPanel(this), ScreenState.LOGIN.name());
        mainPanel.add(new ModeSelectPanel(this), ScreenState.MODE_SELECT.name());
        mainPanel.add(new LobbyPanel(this), ScreenState.LOBBY.name());
        mainPanel.add(new GamePanel(this), ScreenState.GAME.name());
        mainPanel.add(new ResultPanel(this), ScreenState.RESULTS.name());

        add(mainPanel);
        pack();
        setLocationRelativeTo(null);
        showScreen(ScreenState.LOGIN);
    }

    public void showScreen(ScreenState state) {
        cardLayout.show(mainPanel, state.name());
        mainPanel.requestFocusInWindow();
    }
}
