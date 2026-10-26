package tankops.client.ui.render;

import tankops.common.model.common.Vector2D;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Explosion {
    private final Vector2D position;
    private final boolean isBig; // true: nổ xe tăng, false: nổ đạn
    private int currentFrame = 0;
    private final int maxFrames;
    private long lastFrameTime;
    private static final int FRAME_DELAY_MS = 60; // Tốc độ chuyển frame nổ

    public Explosion(Vector2D position, boolean isBig) {
        this.position = position;
        this.isBig = isBig;
        this.maxFrames = isBig ? 5 : 3; // 3 frame nhỏ hoặc 5 frame lớn (3 nhỏ + 2 lớn)
        this.lastFrameTime = System.currentTimeMillis();
    }

    public boolean update() {
        long now = System.currentTimeMillis();
        if (now - lastFrameTime >= FRAME_DELAY_MS) {
            currentFrame++;
            lastFrameTime = now;
        }
        return currentFrame >= maxFrames; // Trả về true khi diễn xong hiệu ứng
    }

    public void render(Graphics2D g2d, SpriteManager spriteManager) {
        String spriteKey;
        int renderSize;

        if (!isBig) {
            // Nổ nhỏ: blast_small_0, blast_small_1, blast_small_2
            spriteKey = "blast_small_" + currentFrame;
            renderSize = 16;
        } else {
            // Nổ lớn: 3 frame small đầu -> 2 frame big
            if (currentFrame < 3) {
                spriteKey = "blast_small_" + currentFrame;
                renderSize = 24;
            } else {
                spriteKey = "blast_big_" + (currentFrame - 3);
                renderSize = 36;
            }
        }

        BufferedImage sprite = spriteManager.getSprite(spriteKey);
        if (sprite != null) {
            int drawX = (int) position.x() - (renderSize / 2);
            int drawY = (int) position.y() - (renderSize / 2);
            g2d.drawImage(sprite, drawX, drawY, renderSize, renderSize, null);
        }
    }
}