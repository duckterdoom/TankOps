package tankops.client.ui.render;

import tankops.client.state.MockGameState;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class GameRenderer {
    private static final int GAME_WIDTH = 800;
    private static final int GAME_HEIGHT = 600;
    private static final int TILE_SIZE = 32;
    private static final int TANK_SIZE = 28;

    private final SpriteManager spriteManager = SpriteManager.getInstance();

    public void render(Graphics2D g2d, MockGameState state, int panelWidth, int panelHeight) {
        long currentTime = System.currentTimeMillis();
        int timeAnimFrame = (int) ((currentTime / 150) % 2);
        int waterFrame = (int) ((currentTime / 150) % 2);

        g2d.setColor(new Color(15, 15, 18));
        g2d.fillRect(0, 0, panelWidth, panelHeight);

        int offsetX = Math.max(0, (panelWidth - GAME_WIDTH) / 2);
        int offsetY = Math.max(0, (panelHeight - GAME_HEIGHT) / 2);

        AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(offsetX, offsetY);

        g2d.setColor(new Color(35, 35, 40));
        g2d.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        renderTilesByLayer(g2d, state.map, 3, waterFrame);
        renderTilesByLayer(g2d, state.map, 4, waterFrame);
        renderTilesByLayer(g2d, state.map, 1, waterFrame);
        renderTilesByLayer(g2d, state.map, 2, waterFrame);

        int enemyFrame = 0;
        int playerFrame = state.isMoving ? timeAnimFrame : 0;

        String enemyDir = "DOWN";
        BufferedImage enemySprite = spriteManager.getEnemySprite("basic", enemyDir, enemyFrame);
        drawTank(g2d, state.enemyX, state.enemyY, enemySprite, Color.RED, state.enemyHp);

        String playerDir = angleToDirection(state.bodyAngle);
        BufferedImage playerSprite = spriteManager.getPlayerSprite(1, 1, playerDir, playerFrame);
        drawTank(g2d, state.tankX, state.tankY, playerSprite, Color.BLUE, state.tankHp);

        for (MockGameState.DummyBullet b : state.bullets) {
            String bulletDir = velocityToDirection(b.dx, b.dy);
            BufferedImage bulletImg = spriteManager.getSprite("bullet_" + bulletDir.toLowerCase());

            if (bulletImg != null) {
                g2d.drawImage(bulletImg, (int) b.x - 6, (int) b.y - 6, 12, 12, null);
            } else {
                g2d.setColor(Color.YELLOW);
                g2d.fillOval((int) b.x - 4, (int) b.y - 4, 8, 8);
            }
        }

        renderTilesByLayer(g2d, state.map, 5, waterFrame);

        g2d.setColor(new Color(60, 60, 70));
        g2d.drawRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        g2d.setTransform(oldTransform);

        renderHUDOverlay(g2d, panelWidth);
    }

    private void renderTilesByLayer(Graphics2D g2d, int[][] map, int targetTileType, int waterFrame) {
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                int tile = map[row][col];
                if (tile != targetTileType) continue;

                int x = col * TILE_SIZE;
                int y = row * TILE_SIZE;
                BufferedImage tileImg = null;

                switch (tile) {
                    case 1:
                        tileImg = spriteManager.getSprite("brick_full");
                        break;
                    case 2:
                        tileImg = spriteManager.getSprite("steel_full");
                        break;
                    case 3:
                        tileImg = spriteManager.getSprite("ice");
                        break;
                    case 4:
                        tileImg = spriteManager.getSprite("water_f" + waterFrame);
                        break;
                    case 5:
                        tileImg = spriteManager.getSprite("trees");
                        break;
                }

                if (tileImg != null) {
                    g2d.drawImage(tileImg, x, y, TILE_SIZE, TILE_SIZE, null);
                }
            }
        }
    }

    private void drawTank(Graphics2D g2d, double x, double y, BufferedImage tankSprite, Color fallbackColor, int hp) {
        int drawX = (int) x - (TANK_SIZE /2);
        int drawY = (int) y - (TANK_SIZE /2);

        if (tankSprite != null) {
            g2d.drawImage(tankSprite, drawX, drawY, TANK_SIZE, TANK_SIZE, null);

        } else {
            g2d.setColor(fallbackColor);
            g2d.fillRect(drawX, drawY, TANK_SIZE, TANK_SIZE);
        }

        if (hp > 0) {
            int barWidth = 40;
            int barHeight = 4;
            int barX = (int) x - (barWidth / 2);
            int barY = drawY - 8;

            int currentHpWidth = (int) (barWidth * (Math.max(0, hp) / 100.0));

            g2d.setColor(Color.RED);
            g2d.fillRect(barX, barY, barWidth, barHeight);

            g2d.setColor(Color.GREEN);
            g2d.fillRect(barX, barY, currentHpWidth, barHeight);
        }
    }

    private String angleToDirection(double angle) {
        double normalized = (angle % 360 + 360) % 360;
        if (normalized >= 45 && normalized < 135)  return "RIGHT";
        if (normalized >= 135 && normalized < 225) return "DOWN";
        if (normalized >= 225 && normalized < 315) return "LEFT";
        return "UP";
    }

    private String velocityToDirection(double vx, double vy) {
        if (Math.abs(vx) > Math.abs(vy)) {
            return vx > 0 ? "RIGHT" : "LEFT";
        } else {
            return vy > 0 ? "DOWN" : "UP";
        }
    }

    private void renderHUDOverlay(Graphics2D g2d, int panelWidth) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Impact", Font.PLAIN, 18));
        g2d.drawString("SCORE: 500", 25, 30);
        g2d.drawString("TIME: 02:45", panelWidth / 2 - 45, 30);
        g2d.drawString("PLAYERS: 2/5", panelWidth - 140, 30);
    }
}
