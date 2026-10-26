package tankops.client.sound;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Quản lý phát hiệu ứng âm thanh (SFX) cho game.
 * Sử dụng ThreadPool để phát âm thanh asynchronously mà không làm đơ Game Loop.
 */
public class SoundManager {
    private static SoundManager instance;
    private final ExecutorService soundPool = Executors.newFixedThreadPool(6);

    private SoundManager() {}

    public static synchronized SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    /**
     * Phát tệp âm thanh theo tên file trong thư mục resources/sounds/
     * @param soundFile Tên file âm thanh (Ví dụ: "shoot.wav", "bonus_obtained.wav")
     */
    public void playSound(String soundFile) {
        soundPool.submit(() -> {
            try {
                String path = "/sounds/" + soundFile;
                InputStream audioSrc = getClass().getResourceAsStream(path);

                if (audioSrc == null) {
                    System.err.println("[SoundManager] Không tìm thấy file âm thanh: " + path);
                    return;
                }

                // Wrap trong BufferedInputStream để hỗ trợ AudioSystem mark/reset stream
                InputStream bufferedIn = new BufferedInputStream(audioSrc);
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);

                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);

                // Giải phóng tài nguyên ngay sau khi phát xong clip
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });

                clip.start();
            } catch (Exception e) {
                System.err.println("[SoundManager] Lỗi phát âm thanh " + soundFile + ": " + e.getMessage());
            }
        });
    }
}