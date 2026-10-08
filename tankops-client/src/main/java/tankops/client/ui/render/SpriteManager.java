package tankops.client.ui.render;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class SpriteManager {
    private static final Map<String, BufferedImage> sprites = new HashMap<>();

    public static void loadSprites() {
        try {

        } catch (Exception e) {
            System.err.println("Chua tim thay file anh resources, su dung hinh ve co ban tam thoi");
        }
    }

    public static BufferedImage getSprites(String name) {
        return sprites.get(name);
    }
}
