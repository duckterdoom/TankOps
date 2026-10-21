package tankops.client.state;

import tankops.common.net.game.TankSnapshot;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Bộ đệm nội suy (Interpolation Buffer) để làm mịn chuyển động xe tăng.
 */
public class InterpolationBuffer {

    private static final int MAX_BUFFER_SIZE = 8;

    private record Entry(TankSnapshot snapshot, long receivedAt) {}

    private final Deque<Entry> buffer = new ArrayDeque<>(MAX_BUFFER_SIZE);
    private final String tankId;
    private double interpolatedX;
    private double interpolatedY;

    public InterpolationBuffer(String tankId) {
        this.tankId = tankId;
    }

    public void pushSnapshot(TankSnapshot snapshot) {
        if (snapshot == null) return;

        while (buffer.size() >= MAX_BUFFER_SIZE) {
            buffer.pollFirst();
        }
        buffer.addLast(new Entry(snapshot, System.currentTimeMillis()));
    }

    public void update(long renderDelayMs) {
        if (buffer.isEmpty()) return;

        long targetTime = System.currentTimeMillis() - renderDelayMs;

        Entry prev = null;
        Entry next = null;

        for (Entry e : buffer) {
            if (e.receivedAt() <= targetTime) {
                prev = e;
            } else {
                next = e;
                break;
            }
        }

        if (prev == null) {
            Entry oldest = buffer.peekFirst();
            if (oldest != null) {
                interpolatedX = oldest.snapshot().x();
                interpolatedY = oldest.snapshot().y();
            }
            return;
        }

        if (next == null) {
            interpolatedX = prev.snapshot().x();
            interpolatedY = prev.snapshot().y();
            return;
        }

        double alpha = (double)(targetTime - prev.receivedAt())
                     / (double)(next.receivedAt() - prev.receivedAt());
        alpha = Math.max(0.0, Math.min(1.0, alpha));

        interpolatedX = lerp(prev.snapshot().x(), next.snapshot().x(), alpha);
        interpolatedY = lerp(prev.snapshot().y(), next.snapshot().y(), alpha);
    }

    public double getInterpolatedX() {
        return interpolatedX;
    }

    public double getInterpolatedY() {
        return interpolatedY;
    }

    public Optional<TankSnapshot> getLatestSnapshot() {
        Entry last = buffer.peekLast();
        return last != null ? Optional.of(last.snapshot()) : Optional.empty();
    }

    public String getTankId() {
        return tankId;
    }

    public void clear() {
        buffer.clear();
        interpolatedX = 0;
        interpolatedY = 0;
    }

    public int size() {
        return buffer.size();
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
