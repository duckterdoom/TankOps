package tankops.client.ui.panels;

import tankops.client.state.ClientConfig;
import tankops.client.ui.MainFrame;
import tankops.client.ui.ScreenState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginPanel extends JPanel {
    public LoginPanel(MainFrame mainFrame) {
        setBackground(new Color(25, 25, 30));
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(30, 30, 30, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("TankOps", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Impact", Font.PLAIN, 48));
        titleLabel.setForeground(new Color(240, 190, 40));

        JLabel subTitle = new JLabel("Multiplayer Tank Battle", SwingConstants.CENTER);
        subTitle.setFont(new Font("Arial", Font.BOLD, 12));
        subTitle.setForeground(Color.LIGHT_GRAY);

        JTextField nameField = new JTextField("Player1", 15);
        JTextField hostField = new JTextField("localhost:5555", 15);
        styleTextField(nameField);
        styleTextField(hostField);

        JButton loginBtn = new JButton("ENTER GAME");
        loginBtn.setFont(new Font("Arial", Font.BOLD, 14));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setBackground(new Color(220, 100, 30));
        loginBtn.setFocusPainted(false);
        loginBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginBtn.setPreferredSize(new Dimension(200, 40));

        loginBtn.addActionListener(e -> {
            String username = nameField.getText().trim();
            String serverAdd = hostField.getText().trim();

            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter your name!");
                return;
            }

            ClientConfig.setUsername(username);
            ClientConfig.setServerAddress(serverAdd);

            if (mainFrame.getGameStateStore() != null) {
                mainFrame.getGameStateStore().setAssignedPlayerId(username);
            }

            mainFrame.showScreen(ScreenState.MODE_SELECT);
        });

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(titleLabel, gbc);

        gbc.gridy = 1;
        add(subTitle, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 2; add(createLabel("Player Name:"), gbc);
        gbc.gridx = 1; add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; add(createLabel("Server Address:"), gbc);
        gbc.gridx = 1; add(hostField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        add(loginBtn, gbc);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Arial", Font.BOLD, 13));
        return label;
    }

    private void styleTextField(JTextField field) {
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        field.setBackground(new Color(45, 45, 55));
        field.setForeground(Color.CYAN);
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 100)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
    }
}
