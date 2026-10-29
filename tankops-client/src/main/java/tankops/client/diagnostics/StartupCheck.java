package tankops.client.diagnostics;

import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.swing.SwingUtilities;
import tankops.client.ui.MainFrame;
import tankops.common.map.MapLoader;
import tankops.common.net.codec.MessageCodec;
import tankops.common.net.system.PingMessage;

/** Executed by the packaged runtime before publishing a client build. Never contacts a remote server. */
public final class StartupCheck {
    private StartupCheck() { }

    public static int run() {
        try {
            System.out.println("Runtime: " + System.getProperty("java.runtime.version")
                    + "; " + System.getProperty("os.name") + "; " + System.getProperty("os.arch"));
            if (new MapLoader().loadMap("/maps/map_multi.map") == null
                    || new MapLoader().loadMap("/maps/map_single.map") == null) {
                throw new IllegalStateException("Missing game maps");
            }
            try (InputStream image = StartupCheck.class.getResourceAsStream("/sprites/bullets/bullet_up.png")) {
                if (image == null || ImageIO.read(image) == null) throw new IllegalStateException("Cannot decode sprite");
            }
            try (InputStream sound = StartupCheck.class.getResourceAsStream("/sounds/shoot.wav")) {
                if (sound == null) throw new IllegalStateException("Missing sound");
                try (var audio = AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(sound))) {
                    if (audio.getFormat().getChannels() < 1) throw new IllegalStateException("Invalid sound");
                }
            }
            PingMessage ping = new PingMessage(123);
            if (!ping.equals(MessageCodec.decode(MessageCodec.encode(ping)))) {
                throw new IllegalStateException("Protocol self-check failed");
            }
            // This catches AccessBridge, preferences and actual Swing panel initialization errors.
            // No frame is shown and no match is started.
            SwingUtilities.invokeAndWait(() -> {
                MainFrame frame = new MainFrame();
                frame.dispose();
            });
            System.out.println("PASS: packaged Java, maps, image/audio decoding, protocol and Swing startup.");
            return 0;
        } catch (Throwable failure) {
            failure.printStackTrace();
            return 1;
        }
    }
}
