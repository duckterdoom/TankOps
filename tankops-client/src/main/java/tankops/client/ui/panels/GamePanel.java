package tankops.client.ui.panels;

import tankops.client.controller.SinglePlayerController;
import tankops.client.state.InputHandler;
import tankops.client.ui.MainFrame;
import tankops.client.ui.render.GameRenderer;
import tankops.common.engine.GameState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GamePanel extends JPanel {
    private final MainFrame mainFrame;
    private final GameRenderer renderer;
    private final InputHandler inputHandler;
    private final SinglePlayerController controller;

    private Timer gameLoopTimer;

    public GamePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.renderer = new GameRenderer() ;
        this.controller = new SinglePlayerController();

        setDoubleBuffered(true);
        setFocusable(true);
        setBackground(new Color(15, 15, 18));

        this.inputHandler = new InputHandler(this);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
            }
        });

        controller.initGame();

        setupGameLoop();
    }

    private void setupGameLoop() {
        gameLoopTimer = new Timer(16, e -> {
            controller.handleInput(
                    inputHandler.isUp(),
                    inputHandler.isDown(),
                    inputHandler.isLeft(),
                    inputHandler.isRight(),
                    inputHandler.isShooting()
            );

            controller.update();

            repaint();
        });
        gameLoopTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        GameState gameState = controller.getGameState();

        if (gameState != null) {
            renderer.render(g2d, gameState, controller.getLocalPlayerId(), getWidth(), getHeight());
        }
    }

    public void stopGameLoop() {
        if (gameLoopTimer != null && gameLoopTimer.isRunning()) {
            gameLoopTimer.stop();
        }
    }
}
