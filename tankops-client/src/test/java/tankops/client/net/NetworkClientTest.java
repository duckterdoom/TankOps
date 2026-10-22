package tankops.client.net;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tankops.common.model.common.Direction;
import tankops.common.net.Message;
import tankops.common.net.codec.MessageCodec;
import tankops.common.net.game.InputMessage;
import tankops.common.net.lobby.JoinResponseMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử NetworkClient - Kết nối và truyền nhận bản tin")
class NetworkClientTest {

    @AfterEach
    void tearDown() {
        NetworkClient.getInstance().disconnect();
        NetworkClient.resetForTesting();
    }

    @Test
    @DisplayName("Kết nối thất bại khi port không tồn tại -> trả về false")
    void connect_failedPort_returnsFalse() {
        NetworkClient client = NetworkClient.getInstance();
        boolean success = client.connect("127.0.0.1", 59999);
        assertFalse(success);
        assertFalse(client.isConnected());
    }

    @Test
    @DisplayName("Kết nối TCP thật và nhận bản tin từ server qua MessageListener")
    void connectAndReceiveMessage_success() throws Exception {
        CountDownLatch connectedLatch = new CountDownLatch(1);
        CountDownLatch messageLatch = new CountDownLatch(1);
        AtomicReference<Message> receivedMsg = new AtomicReference<>();

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();

            Thread serverThread = new Thread(() -> {
                try {
                    Socket clientSocket = serverSocket.accept();
                    DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                    DataInputStream in = new DataInputStream(clientSocket.getInputStream());

                    Message clientMsg = MessageCodec.readMessage(in);
                    if (clientMsg instanceof InputMessage) {
                        MessageCodec.writeMessage(out, new JoinResponseMessage(true, "OK", "room-1", "player-1", 1));
                    }
                } catch (Exception ignored) {}
            });
            serverThread.setDaemon(true);
            serverThread.start();

            NetworkClient client = NetworkClient.getInstance();
            client.addListener(new MessageListener() {
                @Override
                public void onConnected(String host, int port) {
                    connectedLatch.countDown();
                }

                @Override
                public void onMessageReceived(Message message) {
                    receivedMsg.set(message);
                    messageLatch.countDown();
                }
            });

            boolean connected = client.connect("127.0.0.1", port);
            assertTrue(connected);
            assertTrue(client.isConnected());
            assertTrue(connectedLatch.await(2, TimeUnit.SECONDS));

            boolean sent = client.sendMessage(new InputMessage("player-1", 1, Direction.UP, false));
            assertTrue(sent);

            assertTrue(messageLatch.await(3, TimeUnit.SECONDS));
            assertNotNull(receivedMsg.get());
            assertInstanceOf(JoinResponseMessage.class, receivedMsg.get());
            JoinResponseMessage resp = (JoinResponseMessage) receivedMsg.get();
            assertTrue(resp.success());
            assertEquals("room-1", resp.roomId());

            client.disconnect();
            assertFalse(client.isConnected());
        }
    }
}
