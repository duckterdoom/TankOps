package tankops.client.ui.render;

import tankops.common.engine.GameState;
import tankops.common.model.common.Bullet;
import tankops.common.model.common.Direction;
import tankops.common.model.common.Vector2D;
import tankops.common.model.environment.*;
import tankops.common.model.tank.Tank;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class GameRenderer {
    private static final int GAME_WIDTH = 800;
    private static final int GAME_HEIGHT = 576;
    private static final int TILE_SIZE = 32;
    private static final int TANK_SIZE = 32;

    private final SpriteManager spriteManager = SpriteManager.getInstance();

    public void render(Graphics2D g2d, GameState state, String localPlayerId, int panelWidth, int panelHeight) {
        long currentTime = System.currentTimeMillis();
        int animFrame = (int) ((currentTime / 150) % 2);

        g2d.setColor(new Color(15, 15, 18));
        g2d.fillRect(0, 0, panelWidth, panelHeight);

        int offsetX = Math.max(0, (panelWidth - GAME_WIDTH) / 2);
        int offsetY = Math.max(0, (panelHeight - GAME_HEIGHT) / 2);

        AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(offsetX, offsetY);

        g2d.setColor(new Color(35, 35, 40));
        g2d.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        renderEnvironmentByLayer(g2d, state, Ice.class, animFrame);
        renderEnvironmentByLayer(g2d, state, Water.class, animFrame);
        renderEnvironmentByLayer(g2d, state, BrickWall.class, animFrame);
        renderEnvironmentByLayer(g2d, state, SteelWall.class, animFrame);
        renderTanks(g2d, state, localPlayerId, animFrame);
        renderBullets(g2d, state);
        renderEnvironmentByLayer(g2d, state, Trees.class, animFrame);

        g2d.setColor(new Color(60, 60, 70));
        g2d.drawRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        g2d.setTransform(oldTransform);

        renderHUDOverlay(g2d, panelWidth);
    }

    private void renderEnvironmentByLayer(Graphics2D g2d, GameState state, Class<? extends Environment> envClass, int animFrame) {
        if (state.getEnvironments() == null) return;
        for (Environment env : state.getEnvironments()) {
            if (envClass.isInstance(env)) {
                Vector2D pos = env.getPosition();
                BufferedImage tileImg = getEnvironmentSprite(env, animFrame);

                if (tileImg != null) {
                    g2d.drawImage(tileImg, (int) pos.x(), (int) pos.y(), TILE_SIZE, TILE_SIZE, null);
                }
            }
        }
    }

    private BufferedImage getEnvironmentSprite(Environment env, int animFrame) {
        if (env instanceof BrickWall) return spriteManager.getSprite("brick_full");
        if (env instanceof SteelWall) return spriteManager.getSprite("steel_full");
        if (env instanceof Ice) return spriteManager.getSprite("ice");
        if (env instanceof Water) return spriteManager.getSprite("water_f" + animFrame);
        if (env instanceof Trees) return spriteManager.getSprite("trees");
        return null;
    }

    private void renderTanks(Graphics2D g2d, GameState gameState, String localPlayerId, int animFrame) {
        if (gameState.getTanks() == null) return;

        for (Tank tank : gameState.getTanks()) {
            if (tank.isDestroyed()) continue;

            Vector2D pos = tank.getPosition();
            Direction dir = tank.getDirection();
            String dirStr = dir != null ? dir.name() : "UP";

            boolean isLocalPlayer = tank.getId().equals(localPlayerId);

            int frame = tank.isMoving() ? animFrame : 0;

            BufferedImage tankSprite;
            Color fallbackColor;

            if (isLocalPlayer) {
                tankSprite = spriteManager.getPlayerSprite(1, 1, dirStr, frame);
                fallbackColor = Color.BLUE;
            } else {
                tankSprite = spriteManager.getEnemySprite("basic", dirStr, frame);
                fallbackColor = Color.RED;
            }

            drawTank(g2d, pos.x(), pos.y(), tankSprite, fallbackColor, tank.getHealth(), tank.getMaxHealth());
        }
    }

    private void drawTank(Graphics2D g2d, double x, double y, BufferedImage tankSprite, Color fallbackColor, int health, int maxHealth) {
        int drawX = (int) x;
        int drawY = (int) y;

        if (tankSprite != null) {
            g2d.drawImage(tankSprite, drawX, drawY, TANK_SIZE, TANK_SIZE, null);
        } else {
            g2d.setColor(fallbackColor);
            g2d.fillRect(drawX, drawY, TANK_SIZE, TANK_SIZE);
        }

        if (health > 0 && maxHealth > 0) {
            int barWidth = TANK_SIZE;
            int barHeight = 4;
            int barX = drawX;
            int barY = drawY - 6;

            int currentHpWidth = (int) (barWidth * ((double) health / maxHealth));

            g2d.setColor(Color.RED);
            g2d.fillRect(barX, barY, barWidth, barHeight);

            g2d.setColor(Color.GREEN);
            g2d.fillRect(barX, barY, currentHpWidth, barHeight);
        }
    }

    private void renderBullets(Graphics2D g2d, GameState gameState) {
        if (gameState.getBullets() == null) return;

        for (Bullet b : gameState.getBullets()) {
            Vector2D pos = b.getPosition();
            Direction dir = b.getDirection();
            String dirStr = dir != null ? dir.name().toLowerCase() : "up";

            BufferedImage bulletImg = spriteManager.getSprite("bullet_" + dirStr);

            if (bulletImg != null) {
                g2d.drawImage(bulletImg, (int) pos.x() - 4, (int) pos.y() - 4, 8, 8, null);
            } else {
                g2d.setColor(Color.YELLOW);
                g2d.fillOval((int) pos.x() - 3, (int) pos.y() - 3, 6, 6);
            }
        }
    }

    private void renderHUDOverlay(Graphics2D g2d, int panelWidth) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Impact", Font.PLAIN, 18));
        g2d.drawString("SINGLE PLAYER MODE", 25, 30);
        g2d.drawString("OFFLINE", panelWidth - 120, 30);
    }
}
