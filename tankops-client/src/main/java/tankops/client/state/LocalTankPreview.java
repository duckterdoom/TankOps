package tankops.client.state;

import java.util.List;
import java.util.function.LongSupplier;
import tankops.common.model.common.Direction;
import tankops.common.model.environment.Environment;
import tankops.common.model.environment.Ice;
import tankops.common.model.tank.Tank;
import tankops.common.net.game.TankSnapshot;

/** Bounded visual anticipation, not authoritative simulation or input-ACK reconciliation. EDT only. */
public final class LocalTankPreview {
    private static final double MAX_LEAD = 12;
    private static final double PIXELS_PER_SECOND = Tank.MOVE_NORMAL * 30;
    private final LongSupplier clock;
    private TankSnapshot previous;
    private int previousTick;
    private long lastFrame;
    private long lastReceived;
    private double x, y;

    public LocalTankPreview() { this(System::nanoTime); }
    LocalTankPreview(LongSupplier clock) { this.clock = clock; }
    public void clear() { previous = null; }

    public TankSnapshot sample(TankSnapshot authoritative, int tick, Direction input,
                               List<Environment> terrain, double width, double height) {
        long now = clock.getAsLong();
        boolean reset = previous == null || !previous.id().equals(authoritative.id()) || tick < previousTick
                || previous.lives() != authoritative.lives() || authoritative.destroyed()
                || Math.hypot(authoritative.x() - previous.x(), authoritative.y() - previous.y()) > 64;
        double elapsed = reset ? 0 : Math.clamp((now - lastFrame) / 1e9, 0, 0.05);
        if (reset) { x = authoritative.x(); y = authoritative.y(); lastReceived = now; }
        if (previous == null || tick != previousTick) {
            lastReceived = now;
            if (input == null && !authoritative.moving()) { x = authoritative.x(); y = authoritative.y(); }
        }
        previous = authoritative;
        previousTick = tick;
        lastFrame = now;

        // Ice has additional server-side inertia; do not invent a second simulation for it.
        if (authoritative.destroyed() || onIce(authoritative.x(), authoritative.y(), terrain)
                || onIce(x, y, terrain) || blocked(x, y, terrain, width, height)) {
            x = authoritative.x(); y = authoritative.y();
            return authoritative;
        }
        if (input != null && now - lastReceived <= 250_000_000L) {
            // Follow authoritative progress if it overtakes the visual preview.
            if (input.getDx() != 0) {
                y = authoritative.y();
                if ((x - authoritative.x()) * input.getDx() < 0) x = authoritative.x();
            } else {
                x = authoritative.x();
                if ((y - authoritative.y()) * input.getDy() < 0) y = authoritative.y();
            }
            double distance = PIXELS_PER_SECOND * elapsed;
            while (distance > 0) {
                double step = Math.min(1, distance);
                double nextX = x + input.getDx() * step;
                double nextY = y + input.getDy() * step;
                if (Math.hypot(nextX - authoritative.x(), nextY - authoritative.y()) > MAX_LEAD
                        || blocked(nextX, nextY, terrain, width, height) || onIce(nextX, nextY, terrain)) break;
                x = nextX; y = nextY; distance -= step;
            }
        }
        // Never let a stalled or rejected input move the visual tank indefinitely away from the server.
        if (Math.hypot(x - authoritative.x(), y - authoritative.y()) > MAX_LEAD
                || blocked(x, y, terrain, width, height)) { x = authoritative.x(); y = authoritative.y(); }
        return new TankSnapshot(authoritative.id(), x, y,
                input != null ? input : authoritative.direction(), authoritative.health(), authoritative.maxHealth(),
                authoritative.starLevel(), authoritative.shielded(), input != null || authoritative.moving(),
                authoritative.destroyed(), authoritative.lives(), authoritative.score());
    }

    private static boolean onIce(double x, double y, List<Environment> terrain) {
        for (Environment tile : terrain) if (tile instanceof Ice && intersects(x, y, tile)) return true;
        return false;
    }
    private static boolean blocked(double x, double y, List<Environment> terrain, double width, double height) {
        if (x < 0 || y < 0 || x + Tank.TANK_SIZE > width || y + Tank.TANK_SIZE > height) return true;
        for (Environment tile : terrain) {
            if (!tile.isDestroyed() && !tile.canTankPass() && intersects(x, y, tile)) return true;
        }
        return false;
    }
    private static boolean intersects(double x, double y, Environment tile) {
        return x < tile.getPosition().x() + tile.getWidth() && x + Tank.TANK_SIZE > tile.getPosition().x()
                && y < tile.getPosition().y() + tile.getHeight() && y + Tank.TANK_SIZE > tile.getPosition().y();
    }
}
