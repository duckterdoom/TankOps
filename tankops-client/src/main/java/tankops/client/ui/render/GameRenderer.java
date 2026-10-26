package tankops.client.ui.render;

import tankops.common.engine.GameState;
import tankops.common.model.common.Bullet;
import tankops.common.model.common.Direction;
import tankops.common.model.common.Vector2D;
import tankops.common.model.environment.*;
import tankops.common.model.powerups.*;
import tankops.common.model.tank.PlayerTank;
import tankops.common.model.tank.Tank;
import tankops.common.net.game.BulletSnapshot;
import tankops.common.net.game.TankSnapshot;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class GameRenderer {
    private static final int GAME_WIDTH = 800;
    private static final int GAME_HEIGHT = 608;
    private static final int TILE_SIZE = 32;
    private static final int TANK_SIZE = 26;
    private static final int BULLET_SIZE = 4;
    private static final int POWERUP_SIZE = 32;

    private final SpriteManager spriteManager = SpriteManager.getInstance();
    // Danh sách quản lý các hiệu ứng nổ đang diễn ra trên màn hình
    private final List<Explosion> activeExplosions = new ArrayList<>();

    // Lưu vết trạng thái frame trước để phát hiện đạn/xe bị hủy
    private final List<String> previousBulletIds = new ArrayList<>();
    private final List<String> previousDestroyedTankIds = new ArrayList<>();
    // Map lưu vết tọa độ của các viên đạn ở frame trước
    private final java.util.Map<String, Vector2D> previousBulletPositions = new java.util.HashMap<>();

    public void render(Graphics2D g2d, GameState state, String localPlayerId, int panelWidth, int panelHeight) {
        long currentTime = System.currentTimeMillis();
        int animFrame = (int) ((currentTime / 150) % 2);

        detectExplosions(state);

        g2d.setColor(new Color(15, 15, 18));
        g2d.fillRect(0, 0, panelWidth, panelHeight);

        int offsetX = Math.max(0, (panelWidth - GAME_WIDTH) / 2);
        int offsetY = Math.max(0, (panelHeight - GAME_HEIGHT) / 2);

        AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(offsetX, offsetY);

        g2d.setColor(new Color(35, 35, 40));
        g2d.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        // Render thứ tự lớp màn chơi
        renderEnvironmentByLayer(g2d, state, Ice.class, animFrame);
        renderEnvironmentByLayer(g2d, state, Water.class, animFrame);
        renderEnvironmentByLayer(g2d, state, BrickWall.class, animFrame);
        renderEnvironmentByLayer(g2d, state, SteelWall.class, animFrame);

        // 🟢 VẼ POWERUPS TRÊN BẢN ĐỒ
        renderPowerUps(g2d, state, animFrame);

        renderTanks(g2d, state, localPlayerId, animFrame);
        renderBullets(g2d, state);

        // 💥 VẼ HIỆU ỨNG NỔ VÀ CẬP NHẬT FRAME
        renderExplosions(g2d);

        renderEnvironmentByLayer(g2d, state, Trees.class, animFrame);


        g2d.setColor(new Color(60, 60, 70));
        g2d.drawRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        g2d.setTransform(oldTransform);

        renderHUDOverlay(g2d, state, localPlayerId, panelWidth);
    }

    private void detectExplosions(GameState state) {
        if (state == null) return;

        // 1. Kiểm tra xe tăng nổ (Nổ lớn)
        if (state.getTanks() != null) {
            for (Tank tank : state.getTanks()) {
                if (tank.isDestroyed() && !previousDestroyedTankIds.contains(tank.getId())) {
                    previousDestroyedTankIds.add(tank.getId());
                    Vector2D center = new Vector2D(tank.getPosition().x() + 13, tank.getPosition().y() + 13);
                    activeExplosions.add(new Explosion(center, true));
                }
            }
        }

        // 2. Kiểm tra đạn nổ khi chạm tường / chạm địch (Nổ nhỏ)
        java.util.Map<String, Vector2D> currentBulletPositions = new java.util.HashMap<>();
        if (state.getBullets() != null) {
            for (Bullet b : state.getBullets()) {
                currentBulletPositions.put(b.getId(), b.getPosition());
            }
        }

        // Kiểm tra viên đạn nào ở frame trước có mà frame này đã biến mất
        for (java.util.Map.Entry<String, Vector2D> entry : previousBulletPositions.entrySet()) {
            String bulletId = entry.getKey();
            if (!currentBulletPositions.containsKey(bulletId)) {
                Vector2D lastPos = entry.getValue();
                // 🟢 ĐÃ BỔ SUNG: Sinh ra nổ nhỏ (blast_small) ngay tại vị trí viên đạn biến mất
                activeExplosions.add(new Explosion(lastPos, false));
            }
        }

        // Cập nhật lại danh sách đạn cho frame tiếp theo
        previousBulletPositions.clear();
        previousBulletPositions.putAll(currentBulletPositions);
    }

    // Hàm gọi thủ công nếu muốn tạo nổ tại tọa độ bất kỳ
    public void addExplosion(Vector2D position, boolean isBig) {
        activeExplosions.add(new Explosion(position, isBig));
    }

    private void renderExplosions(Graphics2D g2d) {
        Iterator<Explosion> iterator = activeExplosions.iterator();
        while (iterator.hasNext()) {
            Explosion explosion = iterator.next();
            explosion.render(g2d, spriteManager);
            if (explosion.update()) {
                iterator.remove(); // Xóa sau khi chạy hết chuỗi animation
            }
        }
    }

    private void renderPowerUps(Graphics2D g2d, GameState state, int animFrame) {
        if (state == null || state.getPowerUps() == null) return;

        for (PowerUp p : state.getPowerUps()) {
            Vector2D pos = p.getPosition();
            int x = (int) pos.x();
            int y = (int) pos.y();

            String spriteKey = getPowerUpSpriteKey(p);
            BufferedImage sprite = spriteManager.getSprite(spriteKey);

            if (sprite != null) {
                g2d.drawImage(sprite, x, y, POWERUP_SIZE, POWERUP_SIZE, null);
            } else {
                // Fallback nếu chưa có sprite ảnh trong SpriteManager
                drawFallbackPowerUp(g2d, p, x, y);
            }
        }
    }

    private String getPowerUpSpriteKey(PowerUp p) {
        if (p instanceof Helmet) return "powerup_helmet";
        if (p instanceof Grenade) return "powerup_grenade";
        if (p instanceof Star) return "powerup_star";
        if (p instanceof TankPowerUp) return "powerup_tank";
        if (p instanceof Timer) return "powerup_timer";
        if (p instanceof Shovel) return "powerup_shovel";
        return "powerup_default";
    }

    private void drawFallbackPowerUp(Graphics2D g2d, PowerUp p, int x, int y) {
        // Vẽ khung nền hộp quà
        g2d.setColor(new Color(30, 30, 30, 220));
        g2d.fillRoundRect(x, y, POWERUP_SIZE, POWERUP_SIZE, 8, 8);
        g2d.setColor(Color.WHITE);
        g2d.drawRoundRect(x, y, POWERUP_SIZE, POWERUP_SIZE, 8, 8);

        g2d.setFont(new Font("Consolas", Font.BOLD, 14));

        if (p instanceof Helmet) {
            g2d.setColor(Color.CYAN);
            g2d.fillOval(x + 4, y + 4, 24, 24);
            g2d.setColor(Color.BLACK);
            g2d.drawString("H", x + 11, y + 21);
        } else if (p instanceof Grenade) {
            g2d.setColor(Color.RED);
            g2d.fillOval(x + 4, y + 4, 24, 24);
            g2d.setColor(Color.WHITE);
            g2d.drawString("G", x + 11, y + 21);
        } else if (p instanceof Star) {
            g2d.setColor(Color.YELLOW);
            g2d.fillOval(x + 4, y + 4, 24, 24);
            g2d.setColor(Color.BLACK);
            g2d.drawString("S", x + 11, y + 21);
        } else if (p instanceof TankPowerUp) {
            g2d.setColor(Color.GREEN);
            g2d.fillRect(x + 6, y + 6, 20, 20);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Consolas", Font.BOLD, 11));
            g2d.drawString("+1", x + 8, y + 20);
        } else if (p instanceof Timer) {
            g2d.setColor(Color.ORANGE);
            g2d.fillOval(x + 4, y + 4, 24, 24);
            g2d.setColor(Color.BLACK);
            g2d.drawString("T", x + 12, y + 21);
        } else if (p instanceof Shovel) {
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillRect(x + 6, y + 6, 20, 20);
            g2d.setColor(Color.BLACK);
            g2d.drawString("V", x + 11, y + 20);
        }
    }

    private void renderEnvironmentByLayer(Graphics2D g2d, GameState state, Class<? extends Environment> envClass, int animFrame) {
        if (state == null || state.getEnvironments() == null) return;
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
                // 🎲 Lấy loại enemy dựa trên ID xe (đảm bảo ngẫu nhiên nhưng ổn định cho từng xe)
                String enemyType = getEnemyTypeFromId(tank.getId());
                tankSprite = spriteManager.getEnemySprite(enemyType, dirStr, frame);
                fallbackColor = Color.RED;
            }

            drawTank(g2d, pos.x(), pos.y(), tankSprite, fallbackColor, tank.getHealth(), tank.getMaxHealth());

            // 🛡️ VẼ GIÁP SHIELD KHI TANK BẬT HELMET/SHIELD
            if (tank instanceof PlayerTank player && isTankShielded(player)) {
                int drawX = (int) pos.x();
                int drawY = (int) pos.y();

                g2d.setColor(new Color(0, 225, 255, 100)); // Nền Cyan mờ
                g2d.fillOval(drawX - 5, drawY - 5, TANK_SIZE + 10, TANK_SIZE + 10);

                g2d.setColor(Color.CYAN);
                g2d.setStroke(new BasicStroke(2.0f));
                g2d.drawOval(drawX - 5, drawY - 5, TANK_SIZE + 10, TANK_SIZE + 10);
            }
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
            int barHeight = 3;
            int barX = drawX;
            int barY = drawY - 5;

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

            int drawX = (int) pos.x() - (BULLET_SIZE / 2);
            int drawY = (int) pos.y() - (BULLET_SIZE / 2);

            if (bulletImg != null) {
                g2d.drawImage(bulletImg, drawX, drawY, BULLET_SIZE, BULLET_SIZE, null);
            } else {
                g2d.setColor(Color.YELLOW);
                g2d.fillOval((int) pos.x() - 1, (int) pos.y() - 1, 3, 3);
            }
        }
    }

    // 🟢 [UPDATE]: Nhận thêm parameter (GameState state, String localPlayerId) để vẽ số mạng
    // 🟢 [UPDATE SCORE HUD]: Bổ sung hiển thị SCORE và LIVES lên thanh HUD
    private void renderHUDOverlay(Graphics2D g2d, GameState state, String localPlayerId, int panelWidth) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Impact", Font.PLAIN, 18));
        g2d.drawString("SINGLE PLAYER MODE", 25, 30);
        g2d.drawString("OFFLINE", panelWidth - 120, 30);

        // Tìm thông tin xe người chơi để hiển thị LIVES và SCORE
        if (state != null && state.getTanks() != null) {
            for (Tank tank : state.getTanks()) {
                if (tank instanceof PlayerTank player && tank.getId().equals(localPlayerId)) {
                    int lives = getPlayerLives(player);
                    int score = player.getScore(); // Lấy điểm số thực tế của Player

                    // 1. Vẽ số mạng (LIVES)
                    g2d.setColor(new Color(240, 190, 40));
                    g2d.setFont(new Font("Consolas", Font.BOLD, 18));
                    g2d.drawString("LIVES: " + lives, 240, 30);

                    // 2. 🟢 [UPDATE SCORE HUD]: Vẽ điểm số (SCORE) màu xanh lá nổi bật
                    g2d.setColor(new Color(50, 205, 50));
                    g2d.drawString("SCORE: " + score, 360, 30);
                    break;
                }
            }
        }
    }

    // 🟢 [UPDATE]: Hàm phụ trợ đọc thuộc tính lives từ PlayerTank (dùng reflection an toàn nếu getter khác tên)
    private int getPlayerLives(PlayerTank player) {
        try {
            var method = PlayerTank.class.getMethod("getLives");
            return (int) method.invoke(player);
        } catch (Exception e1) {
            try {
                var field = PlayerTank.class.getDeclaredField("lives");
                field.setAccessible(true);
                return field.getInt(player);
            } catch (Exception e2) {
                return player.isDestroyed() ? 0 : 1;
            }
        }
    }

    public void renderSnapshots(Graphics2D g2d, GameState mapState, List<TankSnapshot> tanks,
                                List<BulletSnapshot> bullets, String localPlayerId,
                                int panelWidth, int panelHeight) {
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

        if (mapState != null) {
            renderEnvironmentByLayer(g2d, mapState, Ice.class, animFrame);
            renderEnvironmentByLayer(g2d, mapState, Water.class, animFrame);
            renderEnvironmentByLayer(g2d, mapState, BrickWall.class, animFrame);
            renderEnvironmentByLayer(g2d, mapState, SteelWall.class, animFrame);
            renderPowerUps(g2d, mapState, animFrame);
        }

        if (tanks != null) {
            for (TankSnapshot tank : tanks) {
                if (tank.destroyed()) continue;

                Direction dir = tank.direction();
                String dirStr = dir != null ? dir.name() : "UP";
                boolean isLocalPlayer = tank.id().equals(localPlayerId);
                int frame = tank.moving() ? animFrame : 0;

                BufferedImage tankSprite;
                Color fallbackColor;

                if (isLocalPlayer) {
                    tankSprite = spriteManager.getPlayerSprite(1, 1, dirStr, frame);
                    fallbackColor = Color.BLUE;
                } else {
                    tankSprite = spriteManager.getEnemySprite("basic", dirStr, frame);
                    fallbackColor = Color.RED;
                }

                drawTank(g2d, tank.x(), tank.y(), tankSprite, fallbackColor, tank.health(), tank.maxHealth());
            }
        }

        if (bullets != null) {
            for (BulletSnapshot b : bullets) {
                Direction dir = b.direction();
                String dirStr = dir != null ? dir.name().toLowerCase() : "up";

                BufferedImage bulletImg = spriteManager.getSprite("bullet_" + dirStr);

                int drawX = (int) b.x() - (BULLET_SIZE / 2);
                int drawY = (int) b.y() - (BULLET_SIZE / 2);

                if (bulletImg != null) {
                    g2d.drawImage(bulletImg, drawX, drawY, BULLET_SIZE, BULLET_SIZE, null);
                } else {
                    g2d.setColor(Color.YELLOW);
                    g2d.fillOval((int) b.x() - 1, (int) b.y() - 1, 3, 3);
                }
            }
        }

        if (mapState != null) {
            renderEnvironmentByLayer(g2d, mapState, Trees.class, animFrame);
        }

        g2d.setColor(new Color(60, 60, 70));
        g2d.drawRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        g2d.setTransform(oldTransform);

        renderMultiHUDOverlay(g2d, panelWidth);
    }

    private void renderMultiHUDOverlay(Graphics2D g2d, int panelWidth) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Impact", Font.PLAIN, 18));
        g2d.drawString("MULTIPLAYER MODE", 25, 30);
        g2d.drawString("ONLINE", panelWidth - 120, 30);
    }

    private boolean isTankShielded(PlayerTank player) {
        try {
            var field = PlayerTank.class.getDeclaredField("shielded");
            field.setAccessible(true);
            return field.getBoolean(player);
        } catch (Exception e) {
            return false;
        }
    }

    private String getEnemyTypeFromId(String tankId) {
        if (tankId == null) return "basic";

        String[] types = {"basic", "fast", "power", "armor"};
        // Dùng Math.abs(hashCode) để chọn 1 loại cố định dựa theo ID của xe tăng đó
        int index = Math.abs(tankId.hashCode()) % types.length;
        return types[index];
    }
}