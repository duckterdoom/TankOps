package tankops.client.ui;

import tankops.client.ui.panels.*;

import java.awt.*;
import javax.swing.*;

public class MainFrame extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);

    public MainFrame() {
        setTitle("TankOps");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        mainPanel.add(new LoginPanel(this), ScreenState.LOGIN.name());
        mainPanel.add(new LobbyPanel(this), ScreenState.LOBBY.name());
        mainPanel.add(new GamePanel(this), ScreenState.GAME.name());
        mainPanel.add(new ResultPanel(this), ScreenState.RESULTS.name());

        add(mainPanel);
        showScreen(ScreenState.LOGIN);
    }

    public void showScreen(ScreenState state) {
        cardLayout.show(mainPanel, state.name());
        mainPanel.requestFocusInWindow();
    }
}
