package tankops.client.ui.panels;

import tankops.client.net.NetworkClient;
import tankops.client.state.ClientConfig;
import tankops.client.ui.MainFrame;
import tankops.client.ui.ScreenState;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class ModeSelectPanel extends JPanel {
    public ModeSelectPanel(MainFrame mainFrame) {
        setBackground(new Color(25, 25, 30));
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("SELECT GAME MODE", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Impact", Font.PLAIN, 32));
        titleLabel.setForeground(new Color(240, 190, 40));
        gbc.gridy = 0;
        add(titleLabel, gbc);

        JButton botButton = createStyledButton("Singleplayer", new Color(70, 130, 180));
        botButton.addActionListener(e -> {
            mainFrame.showScreen(ScreenState.GAME);
        });
        gbc.gridy = 1;
        add(botButton, gbc);

        JButton multiButton = createStyledButton("Multiplayer", new Color(46, 139, 87));
        multiButton.addActionListener(e -> {
            String address = ClientConfig.getServerAddress();
            if (address == null || address.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Server address cannot be empty for Multiplayer mode!\nPlease back to Login screen and enter a valid server address.",
                        "Missing Server Address",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean connected = NetworkClient.getInstance().connect(
                    ClientConfig.getHost(),
                    ClientConfig.getPort()
            );

            if (connected) {
                mainFrame.showScreen(ScreenState.LOBBY);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Could not connect to Server at: " + ClientConfig.getServerAddress() + "\nPlease check if server is running!",
                        "Connection error",
                        JOptionPane.ERROR_MESSAGE);
            }


        });
        gbc.gridy = 2;
        add(multiButton, gbc);

        JButton backButton = createStyledButton("Logout", new Color(178, 34, 34));
        backButton.addActionListener(e -> {
            mainFrame.showScreen(ScreenState.LOGIN);
        });
        gbc.gridy = 3;
        add(backButton, gbc);
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bgColor);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(320, 45));
        btn.setBorder(BorderFactory.createRaisedBevelBorder());
        return btn;
    }
}
