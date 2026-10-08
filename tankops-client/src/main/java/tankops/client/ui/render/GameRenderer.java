package tankops.client.ui.render;

import tankops.client.state.MockGameState;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class GameRenderer {
    public void render(Graphics2D g2d, MockGameState state) {
        g2d.setColor(new Color(30, 30, 30));
        g2d.fillRect(0, 0, 800, 600);

        int tileSize = 50;
        for (int row = 0; row < state.map.length; row++) {
            for (int col = 0; col < state.map[row].length; col++) {
                int tile = state.map[row][col];
                if (tile == 1) {
                    g2d.setColor(new Color(180, 80, 50));
                    g2d.fillRect(col * tileSize, row * tileSize, tileSize - 2, tileSize - 2);
                } else if (tile == 2) {
                    g2d.setColor(Color.GRAY);
                    g2d.fillRect(col * tileSize, row * tileSize, tileSize - 2, tileSize - 2);
                }
            }
        }

        g2d.setColor(Color.YELLOW);
        for (MockGameState.DummyBullet b : state.bullets) {
            g2d.fillOval((int) b.x - 4, (int) b.y - 4, 8, 8);
        }

        drawTank(g2d, state.enemyX, state.enemyY, 180, 180, Color.RED, state.enemyHp);

        drawTank(g2d, state.tankX, state.tankY, state.bodyAngle, state.turretAngle, Color.BLUE, state.tankHp);

        renderHUDOverlay(g2d);
    }

    private void drawTank(Graphics2D g2d, double x, double y, double bodyAngle, double turretAngle, Color color, int hp) {
        AffineTransform backup = g2d.getTransform();

        g2d.translate(x, y);
        g2d.rotate(Math.toRadians(bodyAngle));
        g2d.setColor(color);
        g2d.fillRect(-20, -15, 40, 30);

        g2d.setTransform(backup);
        g2d.translate(x, y);
        g2d.rotate(Math.toRadians(turretAngle));
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillOval(-10, -10, 20, 20);
        g2d.fillRect(0, -3, 25, 6);

        g2d.setTransform(backup);

        g2d.setColor(Color.RED);
        g2d.fillRect((int) x - 20, (int) y - 25, 40, 5);
        g2d.setColor(Color.GREEN);
        g2d.fillRect((int) x - 20, (int) y - 25, (int) (40 * (hp / 100.0)), 5);
    }

    private void renderHUDOverlay(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("arial", Font.BOLD, 14));
        g2d.drawString("SCORE: 500", 20, 30);
        g2d.drawString("TIME: 02:45", 360, 30);
        g2d.drawString("PLAYERS: 2/5", 680, 30);
    }
}
