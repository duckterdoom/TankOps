package tankops.client.sound;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý phát hiệu ứng âm thanh (SFX) cho game.
 * Sử dụng ThreadPool để phát âm thanh asynchronously mà không làm đơ Game Loop.
 */
public class SoundManager {
    private static SoundManager instance;
    private final ExecutorService soundPool = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(24), task -> {
                Thread thread = new Thread(task, "TankOps-Audio");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.DiscardPolicy());
    private final AtomicLong lastSlideSound = new AtomicLong();
    private final java.util.Set<String> reportedErrors = ConcurrentHashMap.newKeySet();

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
        if ("sliding.wav".equals(soundFile)) {
            long now = System.nanoTime();
            long last = lastSlideSound.get();
            if ((last != 0 && now - last < 200_000_000L) || !lastSlideSound.compareAndSet(last, now)) return;
        }
        soundPool.submit(() -> {
            Clip clip = null;
            try {
                String path = "/sounds/" + soundFile;
                InputStream audioSrc = getClass().getResourceAsStream(path);

                if (audioSrc == null) {
                    System.err.println("[SoundManager] Không tìm thấy file âm thanh: " + path);
                    return;
                }

                // Wrap trong BufferedInputStream để hỗ trợ AudioSystem mark/reset stream
                try (InputStream bufferedIn = new BufferedInputStream(audioSrc);
                     AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn)) {
                    clip = AudioSystem.getClip();
                    clip.open(audioStream);
                }

                // Giải phóng tài nguyên ngay sau khi phát xong clip
                Clip playing = clip;
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        playing.close();
                    }
                });

                clip.start();
            } catch (Exception e) {
                if (clip != null) clip.close();
                if (reportedErrors.add(soundFile)) {
                    System.err.println("[SoundManager] Lỗi phát âm thanh " + soundFile + ": " + e.getMessage());
                }
            }
        });
    }
}
