package tankops.client.state;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
import tankops.common.net.game.BulletSnapshot;
import tankops.common.net.game.SnapshotMessage;
import tankops.common.net.game.TankSnapshot;

/** Visual interpolation only. Authoritative state and local-player position are never delayed. */
public final class RenderInterpolator {
    private record Frame(SnapshotMessage snapshot, long time) { }
    private final ArrayDeque<Frame> frames = new ArrayDeque<>();
    private final LongSupplier clock;
    private static final long DELAY_NANOS = 66_000_000L;

    public RenderInterpolator() { this(System::nanoTime); }
    RenderInterpolator(LongSupplier clock) { this.clock = clock; }

    public synchronized void clear() { frames.clear(); }

    public synchronized void push(SnapshotMessage snapshot) {
        if (!frames.isEmpty() && snapshot.tick() <= frames.getLast().snapshot().tick()) frames.clear();
        if (frames.size() == 12) frames.removeFirst();
        frames.addLast(new Frame(snapshot, clock.getAsLong()));
    }

    public synchronized SnapshotMessage sample(String localPlayerId) {
        if (frames.isEmpty()) return null;
        SnapshotMessage latest = frames.getLast().snapshot();
        long target = clock.getAsLong() - DELAY_NANOS;
        Frame before = frames.getFirst();
        Frame after = before;
        for (Frame frame : frames) {
            if (frame.time() <= target) { before = frame; after = frame; }
            else { after = frame; break; }
        }
        double alpha = after.time() > before.time()
                ? Math.clamp((double) (target - before.time()) / (after.time() - before.time()), 0, 1) : 1;
        Map<String, TankSnapshot> oldTanks = new HashMap<>();
        Map<String, TankSnapshot> nextTanks = new HashMap<>();
        for (TankSnapshot tank : before.snapshot().tanks()) oldTanks.put(tank.id(), tank);
        for (TankSnapshot tank : after.snapshot().tanks()) nextTanks.put(tank.id(), tank);
        List<TankSnapshot> tanks = new ArrayList<>();
        for (TankSnapshot latestTank : latest.tanks()) {
            TankSnapshot a = oldTanks.get(latestTank.id());
            TankSnapshot b = nextTanks.get(latestTank.id());
            if (latestTank.id().equals(localPlayerId) || latestTank.destroyed() || a == null || b == null
                    || a.destroyed() || b.destroyed() || a.lives() != b.lives() || b.lives() != latestTank.lives()
                    || a.direction() != b.direction() || b.direction() != latestTank.direction()
                    || Math.hypot(b.x() - a.x(), b.y() - a.y()) > 64
                    || Math.hypot(latestTank.x() - b.x(), latestTank.y() - b.y()) > 64) {
                tanks.add(latestTank);
            } else {
                tanks.add(new TankSnapshot(latestTank.id(), lerp(a.x(), b.x(), alpha), lerp(a.y(), b.y(), alpha),
                        latestTank.direction(), latestTank.health(), latestTank.maxHealth(), latestTank.starLevel(),
                        latestTank.shielded(), latestTank.moving(), latestTank.destroyed(), latestTank.lives(), latestTank.score()));
            }
        }
        Map<String, BulletSnapshot> oldBullets = new HashMap<>();
        Map<String, BulletSnapshot> nextBullets = new HashMap<>();
        for (BulletSnapshot bullet : before.snapshot().bullets()) oldBullets.put(bullet.id(), bullet);
        for (BulletSnapshot bullet : after.snapshot().bullets()) nextBullets.put(bullet.id(), bullet);
        List<BulletSnapshot> bullets = new ArrayList<>();
        for (BulletSnapshot latestBullet : latest.bullets()) {
            BulletSnapshot a = oldBullets.get(latestBullet.id());
            BulletSnapshot b = nextBullets.get(latestBullet.id());
            if (a == null || b == null || a.direction() != b.direction()
                    || Math.hypot(b.x() - a.x(), b.y() - a.y()) > 100) bullets.add(latestBullet);
            else bullets.add(new BulletSnapshot(latestBullet.id(), latestBullet.ownerId(),
                    lerp(a.x(), b.x(), alpha), lerp(a.y(), b.y(), alpha), latestBullet.direction()));
        }
        return new SnapshotMessage(latest.tick(), tanks, bullets, latest.gameOver(), latest.winnerId(),
                latest.powerUps(), latest.environments(), List.of());
    }

    private static double lerp(double a, double b, double alpha) { return a + (b - a) * alpha; }
}
