package tankops.common.net;

import org.junit.jupiter.api.Test;
import tankops.common.net.codec.MessageCodec;
import tankops.common.net.codec.MessageWriter;
import tankops.common.net.system.ChatMessage;
import java.io.*;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class MessageWriterTest {
    @Test
    void preservesOrderAndDoesNotDropEvents() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CountDownLatch flushed = new CountDownLatch(3);
        DataOutputStream output = new DataOutputStream(bytes) {
            @Override public void flush() throws IOException { super.flush(); flushed.countDown(); }
        };
        try (MessageWriter writer = new MessageWriter(output, 8, () -> fail("unexpected failure"), "writer-test")) {
            assertTrue(writer.send(new ChatMessage("p", "READY")));
            assertTrue(writer.send(new ChatMessage("p", "/start")));
            assertTrue(writer.send(new ChatMessage("p", "UNREADY")));
            assertTrue(flushed.await(2, TimeUnit.SECONDS));
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
            for (String value : new String[]{"READY", "/start", "UNREADY"}) {
                assertEquals(value, ((ChatMessage) MessageCodec.readMessage(input)).content());
            }
        }
    }

    @Test
    void slowPeerDoesNotBlockCallerAndOverflowClosesConnection() throws Exception {
        CountDownLatch writing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean closed = new AtomicBoolean();
        OutputStream blocked = new OutputStream() {
            @Override public void write(int value) throws IOException {
                writing.countDown();
                try { if (!release.await(2, TimeUnit.SECONDS)) throw new IOException("blocked"); }
                catch (InterruptedException interrupted) { throw new IOException(interrupted); }
            }
        };
        try (MessageWriter writer = new MessageWriter(new DataOutputStream(blocked), 1,
                () -> { closed.set(true); release.countDown(); }, "blocked-writer-test")) {
            assertTrue(writer.send(new ChatMessage("p", "first")));
            assertTrue(writing.await(2, TimeUnit.SECONDS));
            assertTimeoutPreemptively(Duration.ofMillis(500), () -> {
                assertTrue(writer.send(new ChatMessage("p", "second")));
                assertFalse(writer.send(new ChatMessage("p", "overflow")));
            });
            assertTrue(closed.get());
            assertFalse(writer.send(new ChatMessage("p", "after close")));
        } finally { release.countDown(); }
    }
}
