package tankops.client.state;

import org.junit.jupiter.api.Test;
import tankops.common.model.common.Direction;
import tankops.common.net.game.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class RenderInterpolatorTest {
    private TankSnapshot tank(String id, double x, int lives, Direction direction) {
        return new TankSnapshot(id, x, 0, direction, 100, 100, 0, false, true, false, lives, 0);
    }
    private SnapshotMessage frame(int tick, List<TankSnapshot> tanks, List<BulletSnapshot> bullets) {
        return new SnapshotMessage(tick, tanks, bullets, false, null);
    }

    @Test
    void interpolatesRemoteEntitiesButDoesNotDelayLocalPlayer() {
        AtomicLong time = new AtomicLong();
        RenderInterpolator buffer = new RenderInterpolator(time::get);
        buffer.push(frame(1, List.of(tank("local", 0, 3, Direction.RIGHT), tank("remote", 0, 3, Direction.RIGHT)),
                List.of(new BulletSnapshot("b", "remote", 0, 0, Direction.RIGHT))));
        time.set(100_000_000);
        buffer.push(frame(2, List.of(tank("local", 20, 3, Direction.RIGHT), tank("remote", 20, 3, Direction.RIGHT)),
                List.of(new BulletSnapshot("b", "remote", 40, 0, Direction.RIGHT))));
        time.set(116_000_000);
        SnapshotMessage rendered = buffer.sample("local");
        assertEquals(20, rendered.tanks().get(0).x(), 0.001);
        assertEquals(10, rendered.tanks().get(1).x(), 0.001);
        assertEquals(20, rendered.bullets().get(0).x(), 0.001);
        time.set(1_000_000_000);
        assertEquals(20, buffer.sample("local").tanks().get(1).x(), 0.001, "must freeze instead of extrapolating forever");
    }

    @Test
    void doesNotBlendRespawnTeleportOrTurnAndClearsBetweenMatches() {
        AtomicLong time = new AtomicLong();
        RenderInterpolator buffer = new RenderInterpolator(time::get);
        buffer.push(frame(1, List.of(tank("respawn", 0, 3, Direction.RIGHT), tank("teleport", 0, 3, Direction.RIGHT),
                tank("turn", 0, 3, Direction.RIGHT)), List.of()));
        time.set(100_000_000);
        buffer.push(frame(2, List.of(tank("respawn", 20, 2, Direction.RIGHT), tank("teleport", 300, 3, Direction.RIGHT),
                tank("turn", 20, 3, Direction.UP)), List.of()));
        time.set(116_000_000);
        assertEquals(List.of(20.0, 300.0, 20.0), buffer.sample("local").tanks().stream().map(TankSnapshot::x).toList());
        buffer.clear();
        assertNull(buffer.sample("local"));
    }

    @Test
    void removesMissingEntitiesAndResetsWhenTickRestarts() {
        AtomicLong time = new AtomicLong();
        RenderInterpolator buffer = new RenderInterpolator(time::get);
        buffer.push(frame(100, List.of(tank("old", 0, 3, Direction.RIGHT)),
                List.of(new BulletSnapshot("b", "old", 0, 0, Direction.RIGHT))));
        time.set(33_000_000);
        buffer.push(frame(101, List.of(), List.of()));
        assertTrue(buffer.sample("local").tanks().isEmpty());
        assertTrue(buffer.sample("local").bullets().isEmpty());
        buffer.push(frame(0, List.of(tank("new", 500, 3, Direction.RIGHT)), List.of()));
        assertEquals(500, buffer.sample("local").tanks().getFirst().x());
    }
}
