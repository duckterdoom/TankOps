package tankops.client.net;

import tankops.common.net.Message;
import tankops.common.net.codec.MessageCodec;
import tankops.common.net.codec.ProtocolException;
import tankops.common.net.system.PingMessage;
import tankops.common.net.system.PongMessage;
import tankops.common.net.system.DisconnectMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Client mạng TCP cho TankOps - Singleton, thread-safe.
 *
 * Yêu cầu từ tài liệu kiến trúc:
 * - Mục 5.1: "NetworkClient" trong danh sách lớp client
 * - Mục 6.3: "NetworkClient: manages TCP socket, spawns an independent
 *             network-receive thread, dispatches Message objects to listeners"
 * - Mục 6.4: "Protocol: TCP with length-prefixed binary messages"
 *
 * Luồng hoạt động:
 *   Main thread → connect() → tạo socket + luồng receiver
 *   Receiver thread → MessageCodec.readMessage() → notifyListeners()
 *   Any thread → sendMessage() → MessageCodec.writeMessage() (synchronized)
 *
 * Observer pattern: các component (GameStateStore, LobbyPanel) đăng ký
 * MessageListener để nhận sự kiện mạng không đồng bộ.
 */
public final class NetworkClient {

    // ----- Singleton (lazy, không cần đồng bộ hóa vì gọi sớm trên EDT) ------

    private static volatile NetworkClient instance;

    /** Trả về singleton, tạo mới nếu chưa có. */
    public static NetworkClient getInstance() {
        if (instance == null) {
            synchronized (NetworkClient.class) {
                if (instance == null) {
                    instance = new NetworkClient();
                }
            }
        }
        return instance;
    }

    // ----- Trạng thái kết nối ------------------------------------------------

    private volatile Socket         socket;
    private volatile DataInputStream  socketIn;
    private volatile DataOutputStream socketOut;
    private final AtomicBoolean     connected   = new AtomicBoolean(false);

    /** Timeout kết nối TCP: 5 giây theo đặc tả kỹ thuật. */
    private static final int CONNECT_TIMEOUT_MS = 5_000;

    // ----- Danh sách listeners -----------------------------------------------

    /**
     * CopyOnWriteArrayList: an toàn khi iterate trong receiver thread
     * trong khi thread khác thêm/xóa listener.
     */
    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();

    // ----- Luồng nhận dữ liệu ------------------------------------------------

    private volatile Thread receiverThread;

    // ----- Constructor (private — Singleton) ---------------------------------

    private NetworkClient() {}

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Kết nối tới server TankOps.
     *
     * @param host địa chỉ IP/hostname của server
     * @param port cổng TCP của server (thường 5555)
     * @return true nếu kết nối thành công, false nếu thất bại
     */
    public boolean connect(String host, int port) {
        if (connected.get()) {
            System.out.println("[NetworkClient] Đã kết nối rồi, bỏ qua lệnh connect.");
            return true;
        }

        try {
            Socket s = new Socket();
            s.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            s.setTcpNoDelay(true);   // Gửi gói nhỏ ngay, không chờ Nagle buffer
            s.setKeepAlive(true);    // Phát hiện kết nối chết

            this.socket    = s;
            this.socketIn  = new DataInputStream(s.getInputStream());
            this.socketOut = new DataOutputStream(s.getOutputStream());
            connected.set(true);

            startReceiverThread(host, port);
            notifyConnected(host, port);

            System.out.println("[NetworkClient] Kết nối thành công tới " + host + ":" + port);
            return true;

        } catch (IOException e) {
            System.err.println("[NetworkClient] Không thể kết nối tới " + host + ":" + port
                               + " — " + e.getMessage());
            cleanup("Kết nối thất bại: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gửi một Message tới server.
     * Thread-safe: synchronized trên socketOut để tránh xen lẫn dữ liệu.
     *
     * @param message bản tin cần gửi (không null)
     * @return true nếu gửi thành công, false nếu chưa kết nối hoặc lỗi I/O
     */
    public boolean sendMessage(Message message) {
        if (!connected.get()) {
            System.err.println("[NetworkClient] Chưa kết nối, không thể gửi: " + message);
            return false;
        }
        DataOutputStream out = socketOut;
        if (out == null) return false;

        try {
            synchronized (out) {
                MessageCodec.writeMessage(out, message);
            }
            return true;
        } catch (ProtocolException e) {
            System.err.println("[NetworkClient] Lỗi mã hóa message: " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.err.println("[NetworkClient] Lỗi gửi message: " + e.getMessage());
            cleanup("Lỗi I/O khi gửi: " + e.getMessage());
            return false;
        }
    }

    /**
     * Ngắt kết nối khỏi server.
     * Nếu đang kết nối, gửi DisconnectMessage trước khi đóng socket.
     */
    public void disconnect() {
        if (!connected.getAndSet(false)) {
            return; // Đã ngắt rồi
        }

        // Cố gắng thông báo server trước khi đóng socket
        DataOutputStream out = socketOut;
        if (out != null) {
            try {
                synchronized (out) {
                    MessageCodec.writeMessage(out, new DisconnectMessage("", "CLIENT_QUIT"));
                }
            } catch (Exception ignored) {
                // Bỏ qua lỗi khi đang đóng
            }
        }

        closeSocket();
        System.out.println("[NetworkClient] Đã ngắt kết nối.");
    }

    /** @return true nếu đang có kết nối TCP hoạt động tới server */
    public boolean isConnected() {
        return connected.get();
    }

    // =========================================================================
    // LISTENER MANAGEMENT (Observer Pattern)
    // =========================================================================

    /**
     * Đăng ký một MessageListener.
     * Listener sẽ nhận onConnected, onDisconnected, và onMessageReceived.
     *
     * @param listener listener cần đăng ký (không null, không trùng)
     */
    public void addListener(MessageListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Hủy đăng ký một MessageListener.
     *
     * @param listener listener cần xóa
     */
    public void removeListener(MessageListener listener) {
        listeners.remove(listener);
    }

    // =========================================================================
    // RECEIVER THREAD (luồng nền nhận dữ liệu)
    // =========================================================================

    private void startReceiverThread(String host, int port) {
        Thread t = new Thread(() -> {
            System.out.println("[NetworkClient-Receiver] Bắt đầu lắng nghe từ " + host + ":" + port);
            DataInputStream in = socketIn;

            while (connected.get() && in != null) {
                try {
                    Message msg = MessageCodec.readMessage(in);
                    handleIncomingMessage(msg);

                } catch (EOFException e) {
                    System.out.println("[NetworkClient-Receiver] Server đóng kết nối.");
                    break;

                } catch (SocketException e) {
                    if (connected.get()) {
                        System.err.println("[NetworkClient-Receiver] Socket lỗi: " + e.getMessage());
                    }
                    break;

                } catch (ProtocolException e) {
                    System.err.println("[NetworkClient-Receiver] Bản tin không hợp lệ (bỏ qua): "
                                       + e.getMessage());

                } catch (IOException e) {
                    if (connected.get()) {
                        System.err.println("[NetworkClient-Receiver] Lỗi I/O: " + e.getMessage());
                    }
                    break;
                }
            }

            String reason = connected.get() ? "Mất kết nối từ server" : "Ngắt kết nối chủ động";
            cleanup(reason);
            System.out.println("[NetworkClient-Receiver] Dừng. Lý do: " + reason);

        }, "NetworkClient-Receiver");

        t.setDaemon(true);
        t.start();
        this.receiverThread = t;
    }

    private void handleIncomingMessage(Message message) {
        if (message instanceof PingMessage ping) {
            sendMessage(new PongMessage(ping.timestamp()));
            return;
        }

        notifyMessageReceived(message);
    }

    // =========================================================================
    // NOTIFY LISTENERS
    // =========================================================================

    private void notifyConnected(String host, int port) {
        for (MessageListener l : listeners) {
            try {
                l.onConnected(host, port);
            } catch (Exception e) {
                System.err.println("[NetworkClient] Listener.onConnected() lỗi: " + e.getMessage());
            }
        }
    }

    private void notifyDisconnected(String reason) {
        for (MessageListener l : listeners) {
            try {
                l.onDisconnected(reason);
            } catch (Exception e) {
                System.err.println("[NetworkClient] Listener.onDisconnected() lỗi: " + e.getMessage());
            }
        }
    }

    private void notifyMessageReceived(Message message) {
        for (MessageListener l : listeners) {
            try {
                l.onMessageReceived(message);
            } catch (Exception e) {
                System.err.println("[NetworkClient] Listener.onMessageReceived() lỗi: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // CLEANUP
    // =========================================================================

    private void cleanup(String reason) {
        boolean wasConnected = connected.getAndSet(false);
        closeSocket();
        if (wasConnected) {
            notifyDisconnected(reason);
        }
    }

    private void closeSocket() {
        Socket s = socket;
        if (s != null && !s.isClosed()) {
            try {
                s.close();
            } catch (IOException ignored) {}
        }
        socket    = null;
        socketIn  = null;
        socketOut = null;
    }

    static void resetForTesting() {
        if (instance != null) {
            instance.connected.set(false);
            instance.closeSocket();
        }
        instance = null;
    }
}
