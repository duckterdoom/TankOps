package tankops.common.net.roundtrip;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tankops.common.model.common.Direction;
import tankops.common.net.codec.ProtocolException;
import tankops.common.net.game.BulletSnapshot;
import tankops.common.net.game.EventMessage;
import tankops.common.net.game.GameOverMessage;
import tankops.common.net.game.InputMessage;
import tankops.common.net.game.SnapshotMessage;
import tankops.common.net.game.TankSnapshot;
import tankops.common.net.support.NetworkTestSupport;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Kiểm thử Round-Trip: Nhóm Diễn biến Trận đấu (Battle & Gameplay Messages)")
class GameMessageRoundTripTest {

    @Test
    @DisplayName("0x06: InputMessage round-trip (có hướng và bắn)")
    void testInputMessage() throws ProtocolException {
        InputMessage original = new InputMessage("player-1", 100, Direction.DOWN, true);
        InputMessage decoded = NetworkTestSupport.assertRoundTrip(original);
        assertEquals("player-1", decoded.playerId());
        assertEquals(100, decoded.tick());
        assertEquals(Direction.DOWN, decoded.direction());
        assertTrue(decoded.shooting());
    }

    @Test
    @DisplayName("0x06: InputMessage round-trip (đứng yên direction = null)")
    void testInputMessageNullDirection() throws ProtocolException {
        InputMessage original = new InputMessage("player-2", 101, null, false);
        InputMessage decoded = NetworkTestSupport.assertRoundTrip(original);
        assertEquals("player-2", decoded.playerId());
        assertNull(decoded.direction());
        assertFalse(decoded.shooting());
    }

    @Test
    @DisplayName("0x07: SnapshotMessage round-trip (kèm TankSnapshot và BulletSnapshot)")
    void testSnapshotMessage() throws ProtocolException {
        TankSnapshot tank = new TankSnapshot("tank-1", 100.0, 150.0, Direction.UP, 3, 3, 2, true, true, false);
        BulletSnapshot bullet = new BulletSnapshot("b-1", "tank-1", 108.0, 140.0, Direction.UP);
        SnapshotMessage original = new SnapshotMessage(1234, List.of(tank), List.of(bullet), false, null);

        SnapshotMessage decoded = NetworkTestSupport.assertRoundTrip(original);
        assertEquals(1234, decoded.tick());
        assertFalse(decoded.gameOver());
        assertNull(decoded.winnerId());
        assertEquals(1, decoded.tanks().size());
        assertEquals("tank-1", decoded.tanks().get(0).id());
        assertEquals(1, decoded.bullets().size());
        assertEquals("b-1", decoded.bullets().get(0).id());
    }

    @Test
    @DisplayName("0x08: GameOverMessage round-trip")
    void testGameOverMessage() throws ProtocolException {
        GameOverMessage original = new GameOverMessage("player-1", Map.of("player-1", 500, "player-2", 200));
        GameOverMessage decoded = NetworkTestSupport.assertRoundTrip(original);
        assertEquals("player-1", decoded.winnerId());
        assertEquals(500, decoded.scores().get("player-1"));
        assertEquals(200, decoded.scores().get("player-2"));
    }

    @Test
    @DisplayName("0x09: EventMessage round-trip")
    void testEventMessage() throws ProtocolException {
        EventMessage original = new EventMessage(EventMessage.EventType.TANK_DESTROYED, "tank-2", "bullet-1", 50.0, 80.0);
        EventMessage decoded = NetworkTestSupport.assertRoundTrip(original);
        assertEquals(EventMessage.EventType.TANK_DESTROYED, decoded.eventType());
        assertEquals("tank-2", decoded.sourceId());
        assertEquals("bullet-1", decoded.targetId());
        assertEquals(50.0, decoded.x());
        assertEquals(80.0, decoded.y());
    }
}
