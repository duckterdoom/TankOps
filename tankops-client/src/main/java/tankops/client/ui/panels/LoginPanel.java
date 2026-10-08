package tankops.client.ui.panels;

import tankops.client.ui.MainFrame;
import tankops.client.ui.ScreenState;

import javax.swing.*;
import java.awt.*;

public class LoginPanel extends JPanel {
    public LoginPanel(MainFrame mainFrame) {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("TankOps - LOGIN", SwingConstants.CENTER);
        titleLabel.setFont(new Font("arial", Font.BOLD, 24));

        JTextField nameField = new JTextField("Player1", 15);
        JTextField hostField = new JTextField("localhost:5555", 15);
        JButton connectBtn = new JButton("Connect to Lobby");

        connectBtn.addActionListener(e -> mainFrame.showScreen(ScreenState.LOBBY));

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; add(new JLabel("Player Name:"), gbc);
        gbc.gridx = 1; add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; add(new JLabel("Server Address:"), gbc);
        gbc.gridx = 1; add(hostField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        add(connectBtn, gbc);
    }
}
