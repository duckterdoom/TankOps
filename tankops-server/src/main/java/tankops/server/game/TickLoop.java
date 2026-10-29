package tankops.server.game;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * cứ mỗi 1/tickRate giây thì chạy 1 lần công việc (task).
 * Ví dụ tickRate = 30 => 30 lần mỗi giây => mỗi lần cách nhau khoảng 33 ms.
 */
public class TickLoop {

    private final int tickRate;
    private ScheduledExecutorService executor;

    public TickLoop(int tickRate) {
        this.tickRate = tickRate;
    }

    public synchronized void start(Runnable task) {
        if (executor != null) {
            return; // đã chạy rồi thì thôi
        }
        executor = Executors.newSingleThreadScheduledExecutor(); // đúng 1 thread cho vòng tick
        long periodMicros = 1_000_000L / tickRate;

        executor.scheduleAtFixedRate(() -> {
            //  nếu task ném lỗi mà không bắt, ScheduledExecutor sẽ DỪNG HẲN
            // vòng tick và không báo gì. Vì vậy phải try/catch ở đây.
            try {
                task.run();
            } catch (Exception e) {
                System.out.println("[TickLoop] Lỗi trong tick: " + e);
            }
        }, 0, periodMicros, TimeUnit.MICROSECONDS);
    }

    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }
}