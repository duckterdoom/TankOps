package tankops.client.ui.panels;

import tankops.client.state.InputHandler;
import tankops.client.state.MockGameState;
import tankops.client.ui.MainFrame;
import tankops.client.ui.ScreenState;
import tankops.client.ui.render.GameRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class GamePanel extends JPanel {
    private final MockGameState mockState = new MockGameState();
    private final GameRenderer renderer = new GameRenderer();
    private final InputHandler inputHandler;

    public GamePanel(MainFrame mainFrame) {
        setDoubleBuffered(true);
        this.inputHandler = new InputHandler(this);

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "endGame");
        getActionMap().put("endGame", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainFrame.showScreen(ScreenState.RESULTS);
            }
        });

        Timer gameLoop = new Timer(16, e -> {
            mockState.update(inputHandler);
            repaint();
        });
        gameLoop.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        renderer.render(g2d, mockState);
    }
}
