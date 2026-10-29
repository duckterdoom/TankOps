package tankops.client.state;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import tankops.common.model.common.Direction;
import tankops.common.model.common.Vector2D;
import tankops.common.model.environment.BrickWall;
import tankops.common.model.environment.Ice;
import tankops.common.model.environment.Water;
import tankops.common.net.game.TankSnapshot;
import static org.junit.jupiter.api.Assertions.*;

class LocalTankPreviewTest {
    private TankSnapshot tank(double x, int lives, boolean moving) {
        return new TankSnapshot("local", x, 0, Direction.RIGHT, 1, 1, 0, false, moving, false, lives, 0);
    }

    @Test
    void respondsBeforeServerMovesWithoutChangingAuthoritativeState() {
        AtomicLong now = new AtomicLong();
        LocalTankPreview preview = new LocalTankPreview(now::get);
        TankSnapshot server = tank(0, 1, false);
        preview.sample(server, 1, null, List.of(), 800, 600);
        now.set(16_000_000);
        TankSnapshot rendered = preview.sample(server, 1, Direction.RIGHT, List.of(), 800, 600);
        assertEquals(0.96, rendered.x(), 0.001);
        assertEquals(0, server.x());
        assertEquals(server.health(), rendered.health());
    }

    @Test
    void limitsSpeculationAndStopsWhenSnapshotsStall() {
        AtomicLong now = new AtomicLong();
        LocalTankPreview preview = new LocalTankPreview(now::get);
        TankSnapshot server = tank(0, 1, false);
        preview.sample(server, 1, Direction.RIGHT, List.of(), 800, 600);
        double last = 0;
        for (int i = 1; i <= 100; i++) {
            now.set(i * 16_000_000L);
            last = preview.sample(server, 1, Direction.RIGHT, List.of(), 800, 600).x();
            assertTrue(last <= 12);
        }
        assertTrue(last > 0);
        now.set(10_000_000_000L);
        assertEquals(last, preview.sample(server, 1, Direction.RIGHT, List.of(), 800, 600).x());
    }

    @Test
    void cannotCrossWallsWaterOrMapBounds() {
        for (var obstacle : List.of(new BrickWall("wall", new Vector2D(32, 0)),
                new Water("water", new Vector2D(32, 0)))) {
            AtomicLong now = new AtomicLong();
            LocalTankPreview preview = new LocalTankPreview(now::get);
            TankSnapshot server = tank(0, 1, false);
            preview.sample(server, 1, Direction.RIGHT, List.of(obstacle), 800, 600);
            for (int i = 1; i < 12; i++) {
                now.set(i * 16_000_000L);
                assertTrue(preview.sample(server, 1, Direction.RIGHT, List.of(obstacle), 800, 600).x() <= 4);
            }
        }
        AtomicLong now = new AtomicLong();
        LocalTankPreview preview = new LocalTankPreview(now::get);
        preview.sample(tank(0, 1, false), 1, Direction.LEFT, List.of(), 800, 600);
        now.set(16_000_000);
        assertEquals(0, preview.sample(tank(0, 1, false), 1, Direction.LEFT, List.of(), 800, 600).x());
    }

    @Test
    void resynchronizesOnRespawnTeleportNewMatchAndStop() {
        AtomicLong now = new AtomicLong();
        LocalTankPreview preview = new LocalTankPreview(now::get);
        preview.sample(tank(0, 2, true), 10, Direction.RIGHT, List.of(), 800, 600);
        now.set(16_000_000);
        assertTrue(preview.sample(tank(0, 2, true), 10, Direction.RIGHT, List.of(), 800, 600).x() > 0);
        now.set(32_000_000);
        assertEquals(100, preview.sample(tank(100, 1, false), 11, null, List.of(), 800, 600).x());
        now.set(48_000_000);
        assertEquals(0, preview.sample(tank(0, 1, false), 0, null, List.of(), 800, 600).x());
        now.set(64_000_000);
        preview.sample(tank(0, 1, true), 1, Direction.RIGHT, List.of(), 800, 600);
        now.set(80_000_000);
        assertEquals(2, preview.sample(tank(2, 1, false), 2, null, List.of(), 800, 600).x());
    }

    @Test
    void doesNotPredictIceOrDestroyedTanks() {
        AtomicLong now = new AtomicLong();
        LocalTankPreview preview = new LocalTankPreview(now::get);
        TankSnapshot server = tank(0, 1, false);
        var terrain = List.<tankops.common.model.environment.Environment>of(new Ice("ice", new Vector2D(0, 0)));
        preview.sample(server, 1, Direction.RIGHT, terrain, 800, 600);
        now.set(16_000_000);
        assertSame(server, preview.sample(server, 1, Direction.RIGHT, terrain, 800, 600));
        TankSnapshot dead = new TankSnapshot("local", 0, 0, Direction.RIGHT, 0, 1, 0, false, false, true, 0, 0);
        assertSame(dead, preview.sample(dead, 2, Direction.RIGHT, List.of(), 800, 600));
    }
}
