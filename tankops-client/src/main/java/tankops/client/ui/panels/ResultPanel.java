package tankops.client.ui.panels;

import tankops.client.ui.MainFrame;
import tankops.client.ui.ScreenState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class ResultPanel extends JPanel {
    private final JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 10));
    private final MainFrame mainFrame;

    public ResultPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setBackground(new Color(25, 25,30));
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(30, 40, 30, 40));

        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("MATCH FINISHED", SwingConstants.CENTER);
        title.setFont(new Font("Impact", Font.PLAIN, 42));
        title.setForeground(new Color(240, 190, 40));

        JLabel subTitle = new JLabel("RESULT & STATUS", SwingConstants.CENTER);
        subTitle.setFont(new Font("Arial", Font.BOLD, 14));
        subTitle.setForeground(Color.LIGHT_GRAY);

        headerPanel.add(title);
        headerPanel.add(subTitle);
        add(headerPanel, BorderLayout.NORTH);

        String[] columnNames = {"RANK", "PLAYER", "KILLS", "DEATHS", "SCORE", "STATUS"};
        Object[][] data = {
                {"#1", "Player1", 7, 1, 1500, "MVP"},
                {"#2", "TankMaster", 4, 3, 950, "RUNNER-UP"},
                {"#3", "Bot_Alpha", 2, 4, 500, "DEFEATED"},
                {"#4", "Bot_Bravo", 1, 5, 250, "DEFEATED"},
                {"#5", "ShadowTank", 0, 5, 100, "DEFEATED"}
        };

        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
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
