package tankops.client.net;

import org.junit.jupiter.api.Test;
import tankops.client.controller.MultiplayerController;
import tankops.client.state.GameStateStore;
import tankops.client.state.InputHandler;
import tankops.common.model.common.Direction;
import tankops.common.net.codec.MessageCodec;
import tankops.common.net.game.InputMessage;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class MultiplayerInputTest {
    @Test
    void shortPressAndReleaseAreSentEvenBetweenTimerPulses() throws Exception {
        LinkedBlockingQueue<InputMessage> messages = new LinkedBlockingQueue<>();
        AtomicReference<Socket> accepted = new AtomicReference<>();
        AtomicReference<MultiplayerController> controller = new AtomicReference<>();
        NetworkClient.resetForTesting();
        try (ServerSocket server = new ServerSocket(0)) {
            Thread reader = new Thread(() -> {
                try (Socket peer = server.accept()) {
                    accepted.set(peer);
                    DataInputStream input = new DataInputStream(peer.getInputStream());
                    while (!peer.isClosed()) {
                        if (MessageCodec.readMessage(input) instanceof InputMessage message) messages.add(message);
                    }
                } catch (Exception ignored) { }
            });
            reader.setDaemon(true);
            reader.start();
            NetworkClient network = NetworkClient.getInstance();
            assertTrue(network.connect("127.0.0.1", server.getLocalPort()));
            SwingUtilities.invokeAndWait(() -> {
                GameStateStore state = new GameStateStore();
                state.setAssignedPlayerId("local");
                state.setInGame(true);
                JPanel panel = new JPanel();
                InputHandler handler = new InputHandler(panel);
                MultiplayerController current = new MultiplayerController(handler, state);
                controller.set(current);
                current.start();
                // Both events run in the same EDT task: no Swing Timer can run between them.
                panel.getActionMap().get("move_right_press").actionPerformed(new ActionEvent(panel, 0, ""));
                panel.getActionMap().get("shoot_press").actionPerformed(new ActionEvent(panel, 0, ""));
                panel.getActionMap().get("shoot_release").actionPerformed(new ActionEvent(panel, 0, ""));
                panel.getActionMap().get("move_right_release").actionPerformed(new ActionEvent(panel, 0, ""));
                current.stop();
            });
            InputMessage initial = messages.poll(2, TimeUnit.SECONDS);
            InputMessage moving = messages.poll(2, TimeUnit.SECONDS);
            InputMessage firing = messages.poll(2, TimeUnit.SECONDS);
            InputMessage stopFiring = messages.poll(2, TimeUnit.SECONDS);
            InputMessage stopped = messages.poll(2, TimeUnit.SECONDS);
            assertNotNull(initial);
            assertNotNull(moving);
            assertEquals(Direction.RIGHT, moving.direction());
            assertNotNull(firing);
            assertTrue(firing.shooting());
            assertNotNull(stopFiring);
            assertFalse(stopFiring.shooting());
            assertNotNull(stopped);
            assertNull(stopped.direction());
        } finally {
            SwingUtilities.invokeAndWait(() -> { if (controller.get() != null) controller.get().stop(); });
            NetworkClient.resetForTesting();
            Socket peer = accepted.get();
            if (peer != null) peer.close();
        }
    }
}
