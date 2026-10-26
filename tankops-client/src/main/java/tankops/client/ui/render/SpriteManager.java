package tankops.client.ui.render;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class SpriteManager {
    private static SpriteManager instance;
    private static final Map<String, BufferedImage> sprites = new HashMap<>();

    private SpriteManager() {
        loadAllSprites();
    }

    public static synchronized SpriteManager getInstance() {
        if (instance == null) {
            instance = new SpriteManager();
        }
        return instance;
    }

    public void loadAllSprites() {
        loadTile("brick_full");
        loadTile("brick_damaged_1");
        loadTile("brick_damaged_2");
        loadTile("brick_damaged_left");
        loadTile("brick_damaged_right");
        loadTile("brick_half_top");
        loadTile("brick_half_bottom");
        loadTile("steel_full");
        loadTile("steel_damaged_1");
        loadTile("steel_damaged_right");
        loadTile("trees");
        loadTile("ice");
        loadTile("eagle_alive");
        loadTile("eagle_destroyed");

        for (int i = 0; i < 2; i++) {
            loadTile("water_f" + i);
        }

        loadSprite("bullet_up", "/sprites/bullets/bullet_up.png");
        loadSprite("bullet_down", "/sprites/bullets/bullet_down.png");
        loadSprite("bullet_left", "/sprites/bullets/bullet_left.png");
        loadSprite("bullet_right", "/sprites/bullets/bullet_right.png");

        loadSprite("blast_big_0", "/sprites/blast/blast_big_0.png");
        loadSprite("blast_big_1", "/sprites/blast/blast_big_1.png");
        for (int i = 0; i <= 2; i++) {
            loadSprite("blast_small_" + i, "/sprites/blast/blast_small_" + i + ".png");
        }
        for (int i = 0; i <= 3; i++) {
            loadSprite("spawn_f" + i, "/sprites/spawn/spawn_f" + i + ".png");
        }

        String[] directions = {"up", "down", "left", "right"};
        for (int p = 1; p <= 5; p++) {
            for (int lvl = 1; lvl <= 4; lvl++) {
                for (String dir : directions) {
                    for (int f = 0; f <= 1; f++) {
                        String key = String.format("player%d_lvl%d_%s_f%d", p, lvl, dir, f);
                        String path = String.format("/sprites/tanks/player%d/level%d/%s_f%d.png", p, lvl, dir, f);
                        loadSprite(key, path);
                    }
                }
            }
        }

        String[] enemyTypes = {"basic", "fast", "power", "armor"};
        for (String type : enemyTypes) {
            for (String dir : directions) {
                for (int f = 0; f <= 1; f++) {
                    String key = String.format("enemy_%s_%s_f%d", type, dir, f);
                    String path = String.format("/sprites/tanks/enemy/%s/%s_f%d.png", type, dir, f);
                    loadSprite(key, path);
                }
            }
        }

        loadPowerUp("helmet");
        loadPowerUp("grenade");
        loadPowerUp("star");
        loadPowerUp("tank");
        loadPowerUp("timer");
        loadPowerUp("shovel");
    }

    private void loadPowerUp(String name) {
        loadSprite("powerup_" + name, "/sprites/powerups/" + name + ".png");
    }

    private void loadTile(String name) {
        loadSprite(name, "/sprites/tiles/" + name + ".png");
    }

    private void loadSprite(String key, String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is != null) {
                sprites.put(key, ImageIO.read(is));
            } else {
                System.err.println("[SpriteManager] Missing asset: " + path);
            }
        } catch (IOException e) {
            System.err.println("[SpriteManager] Failed to load: " + path);
        }
    }

    public BufferedImage getSprite(String key) {
        return sprites.get(key);
    }

    public BufferedImage getPlayerSprite(int playerNum, int level, String direction, int frame) {
        String key = String.format("player%d_lvl%d_%s_f%d", playerNum, level, direction.toLowerCase(), frame % 2);
        return getSprite(key);
    }

    public BufferedImage getEnemySprite(String enemyType, String direction, int frame) {
        String key = String.format("enemy_%s_%s_f%d", enemyType.toLowerCase(), direction.toLowerCase(), frame % 2);
        return getSprite(key);
    }
}
