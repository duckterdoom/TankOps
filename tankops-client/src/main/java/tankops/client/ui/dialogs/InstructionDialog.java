package tankops.client.ui.dialogs;

import tankops.client.ui.render.SpriteManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

public class InstructionDialog extends JDialog {

    private final SpriteManager spriteManager = SpriteManager.getInstance();

    public InstructionDialog(Frame owner) {
        super(owner, "HOW TO PLAY - TANKOPS", true);

        setSize(650, 520);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(25, 25, 30));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Header Title
        JLabel titleLabel = new JLabel("TANKOPS GAMEPLAY GUIDE", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Impact", Font.PLAIN, 24));
        titleLabel.setForeground(new Color(240, 190, 40));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // TabbedPane for categories
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 13));

        tabbedPane.addTab("Controls", createControlsPanel());
        tabbedPane.addTab("Power-Ups", createPowerUpsPanel());
        tabbedPane.addTab("Terrain", createTerrainPanel());
        tabbedPane.addTab("Game Rules", createRulesPanel());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Close Button
        JButton closeBtn = new JButton("GOT IT");
        closeBtn.setFont(new Font("Arial", Font.BOLD, 14));
        closeBtn.setFocusable(false);
        closeBtn.setBackground(new Color(50, 205, 50));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());

        JPanel southPanel = new JPanel();
        southPanel.setOpaque(false);
        southPanel.add(closeBtn);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createControlsPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 10, 10));
        panel.setBackground(new Color(35, 35, 40));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        panel.add(createRowItem(null, "MOVEMENT:", "W, A, S, D  or  Arrow Keys (▲ ◄ ▼ ►)"));
        panel.add(createRowItem(null, "SHOOT:", "SPACEBAR  or  LEFT MOUSE CLICK"));
        panel.add(createRowItem(null, "READY (LOBBY):", "Click the READY button in Multiplayer Room"));

        return panel;
    }

    private JPanel createPowerUpsPanel() {
        JPanel panel = new JPanel(new GridLayout(6, 1, 5, 5));
        panel.setBackground(new Color(35, 35, 40));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        panel.add(createRowItem(spriteManager.getSprite("powerup_helmet"), "Helmet:", "Grants temporary Cyan invincibility shield"));
        panel.add(createRowItem(spriteManager.getSprite("powerup_grenade"), "Grenade:", "Instantly destroys all active enemy tanks"));
        panel.add(createRowItem(spriteManager.getSprite("powerup_star"), "Star:", "Upgrades tank (Increases speed & fire rate)"));
        panel.add(createRowItem(spriteManager.getSprite("powerup_tank"), "Tank (+1):", "Grants +1 Extra Life (LIVE)"));
        panel.add(createRowItem(spriteManager.getSprite("powerup_timer"), "Timer:", "Freezes all enemy tanks temporarily"));
        panel.add(createRowItem(spriteManager.getSprite("powerup_shovel"), "Shovel:", "Reinforces base walls into steel walls"));

        return panel;
    }

    private JPanel createTerrainPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 1, 5, 5));
        panel.setBackground(new Color(35, 35, 40));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        panel.add(createRowItem(spriteManager.getSprite("brick_full"), "Brick Wall:", "Destructible by bullet fire"));
        panel.add(createRowItem(spriteManager.getSprite("steel_full"), "Steel Wall:", "Indestructible concrete armor"));
        panel.add(createRowItem(spriteManager.getSprite("water_f0"), "Water:", "Tanks cannot cross; bullets pass over"));
        panel.add(createRowItem(spriteManager.getSprite("trees"), "Trees:", "Provides visual cover for tanks"));
        panel.add(createRowItem(spriteManager.getSprite("ice"), "Ice:", "Causes tanks to slide when moving"));

        return panel;
    }

    private JPanel createRulesPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(35, 35, 40));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(new Color(35, 35, 40));
        area.setForeground(Color.WHITE);
        area.setFont(new Font("Arial", Font.PLAIN, 13));
        area.setText(
                "1. SINGLE PLAYER MODE:\n" +
                        "   - Objective: Eliminate all enemy Bot tanks on the map.\n" +
                        "   - Game Over: Runs out of lives (LIVES = 0) or base gets destroyed.\n\n" +
                        "2. MULTIPLAYER MODE:\n" +
                        "   - Lobby: Join a room; all Members must click READY for Host to start.\n" +
                        "   - Scoring: Gain +100 SCORE per enemy tank hit.\n" +
                        "   - Victory: The last surviving player/team on the battlefield wins (WINNER)!"
        );

        panel.add(area);
        return panel;
    }

    private JPanel createRowItem(BufferedImage icon, String title, String desc) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        row.setOpaque(false);

        if (icon != null) {
            JLabel iconLabel = new JLabel(new ImageIcon(icon));
            row.add(iconLabel);
        } else {
            JLabel spacer = new JLabel();
            spacer.setPreferredSize(new Dimension(32, 32));
            row.add(spacer);
        }

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Arial", Font.BOLD, 13));
        titleLbl.setForeground(new Color(240, 190, 40));
        titleLbl.setPreferredSize(new Dimension(130, 30));

        JLabel descLbl = new JLabel(desc);
        descLbl.setFont(new Font("Arial", Font.PLAIN, 12));
        descLbl.setForeground(Color.WHITE);

        row.add(titleLbl);
        row.add(descLbl);

        return row;
    }
}