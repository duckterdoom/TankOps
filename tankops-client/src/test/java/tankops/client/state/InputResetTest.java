package tankops.client.state;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InputResetTest {
    @Test
    void resetReleasesHeldMovementAndFireAndNotifiesSender() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel panel = new JPanel();
            InputHandler input = new InputHandler(panel);
            AtomicInteger changes = new AtomicInteger();
            input.setOnInputChanged(changes::incrementAndGet);
            panel.getActionMap().get("move_up_press").actionPerformed(new ActionEvent(panel, 0, ""));
            panel.getActionMap().get("shoot_press").actionPerformed(new ActionEvent(panel, 0, ""));
            assertNotNull(input.getMovementDirection());
            assertTrue(input.isShooting());
            input.clear();
            assertNull(input.getMovementDirection());
            assertFalse(input.isShooting());
            assertEquals(3, changes.get());
        });
    }
}
