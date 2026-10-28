package tankops.common.net.roundtrip;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tankops.common.model.common.Direction;
import tankops.common.net.codec.ProtocolException;
import tankops.common.net.game.BulletSnapshot;
import tankops.common.net.game.EnvironmentSnapshot;
import tankops.common.net.game.EventMessage;
import tankops.common.net.game.GameOverMessage;
import tankops.common.net.game.InputMessage;
import tankops.common.net.game.PowerUpSnapshot;
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

    @Test
    @DisplayName("0x07: SnapshotMessage round-trip kèm vật phẩm (PowerUpSnapshot) và tường (EnvironmentSnapshot)")
    void testSnapshotMessageWithPowerUpsAndEnvironments() throws ProtocolException {
        TankSnapshot tank = new TankSnapshot("tank-1", 100.0, 150.0, Direction.UP, 3, 3, 2, true, true, false);
        List<PowerUpSnapshot> powerUps = List.of(
                new PowerUpSnapshot("pu-1", "Star", 64.0, 96.0),
                new PowerUpSnapshot("pu-2", "Helmet", 320.0, 32.0));
        List<EnvironmentSnapshot> envs = List.of(
                new EnvironmentSnapshot("env_2_3", "BrickWall", 96.0, 64.0),
                new EnvironmentSnapshot("steel_4_5", "SteelWall", 160.0, 128.0));
        SnapshotMessage original = new SnapshotMessage(
                77, List.of(tank), List.of(), false, null, powerUps, envs);

        SnapshotMessage decoded = NetworkTestSupport.assertRoundTrip(original);
        assertEquals(2, decoded.powerUps().size());
        assertEquals("Star", decoded.powerUps().get(0).kind());
        assertEquals(320.0, decoded.powerUps().get(1).x());
        assertEquals(2, decoded.environments().size());
        assertEquals("SteelWall", decoded.environments().get(1).kind());
        assertEquals("env_2_3", decoded.environments().get(0).id());
    }

    @Test
    @DisplayName("0x07: SnapshotMessage round-trip kèm danh sách âm thanh; bản cũ có danh sách âm thanh rỗng")
    void testSnapshotMessageWithSounds() throws ProtocolException {
        SnapshotMessage original = new SnapshotMessage(
                5, List.of(), List.of(), false, null, List.of(), List.of(),
                List.of("shoot.wav", "tank_exploded.wav"));
        SnapshotMessage decoded = NetworkTestSupport.assertRoundTrip(original);
        assertEquals(List.of("shoot.wav", "tank_exploded.wav"), decoded.sounds());

        SnapshotMessage old = new SnapshotMessage(6, List.of(), List.of(), false, null);
        assertTrue(NetworkTestSupport.assertRoundTrip(old).sounds().isEmpty());
    }

    @Test
    @DisplayName("SnapshotMessage bản 5 tham số (cũ) có danh sách vật phẩm và tường rỗng")
    void testSnapshotMessageOldConstructorHasEmptyLists() throws ProtocolException {
        SnapshotMessage original = new SnapshotMessage(1, List.of(), List.of(), false, null);
        SnapshotMessage decoded = NetworkTestSupport.assertRoundTrip(original);
        assertTrue(decoded.powerUps().isEmpty());
        assertTrue(decoded.environments().isEmpty());
    }

    @Test
    @DisplayName("0x08: GameOverMessage round-trip kèm số kill và số lần bị hạ")
    void testGameOverMessageWithKillsAndDeaths() throws ProtocolException {
        GameOverMessage original = new GameOverMessage("An",
                Map.of("An", 100, "Binh", 0), Map.of("An", 1, "Binh", 0), Map.of("An", 0, "Binh", 1));
        GameOverMessage decoded = NetworkTestSupport.assertRoundTrip(original);
        assertEquals(1, decoded.kills().get("An"));
        assertEquals(1, decoded.deaths().get("Binh"));
        assertEquals(100, decoded.scores().get("An"));
    }

    @Test
    @DisplayName("GameOverMessage bản 2 tham số (cũ) có kill và death rỗng")
    void testGameOverMessageOldConstructor() throws ProtocolException {
        GameOverMessage decoded = NetworkTestSupport.assertRoundTrip(new GameOverMessage("An", Map.of("An", 100)));
        assertTrue(decoded.kills().isEmpty());
        assertTrue(decoded.deaths().isEmpty());
    }
}
