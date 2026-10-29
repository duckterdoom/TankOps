package tankops.common.net.codec;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import tankops.common.net.Message;

/** One bounded, ordered writer per connection; callers never wait for socket I/O. */
public final class MessageWriter implements AutoCloseable {
    private final ArrayBlockingQueue<Message> queue;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Runnable closeConnection;
    private final Thread worker;

    public MessageWriter(DataOutputStream output, int capacity, Runnable closeConnection, String name) {
        queue = new ArrayBlockingQueue<>(capacity);
        this.closeConnection = closeConnection;
        worker = new Thread(() -> {
            try {
                while (running.get()) {
                    MessageCodec.writeMessage(output, queue.take());
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } catch (IOException failure) {
                fail();
            }
        }, name);
        worker.setDaemon(true);
        worker.start();
    }

    /** Accepted for ordered delivery, not an acknowledgement from the peer. */
    public boolean send(Message message) {
        if (!running.get()) return false;
        if (queue.offer(message)) return true;
        // Never silently discard START/GAME OVER, short fire presses or snapshot sounds.
        fail();
        return false;
    }

    private void fail() {
        if (running.getAndSet(false)) {
            queue.clear();
            closeConnection.run(); // Must close the socket to release a blocked read/write.
            worker.interrupt();
        }
    }

    @Override
    public void close() {
        running.set(false);
        queue.clear();
        worker.interrupt();
    }
}
