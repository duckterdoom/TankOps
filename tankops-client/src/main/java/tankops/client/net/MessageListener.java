package tankops.client.net;

import tankops.common.net.Message;

/**
 * Interface cho các component của Client đăng ký nhận bản tin từ Server.
 *
 * Yêu cầu từ tài liệu:
 * - Mục 5.1: "MessageListener" trong danh sách interfaces
 * - Mục 6.3: "an independent network-receive thread" nhận và phân phối sự kiện
 *
 * Triển khai Observer Pattern: GameStateStore, LobbyPanel thực thi interface này
 * và đăng ký với NetworkClient.
 */
public interface MessageListener {

    /**
     * Gọi khi nhận được một bản tin hợp lệ từ Server.
     * Chạy trên luồng network-receive, KHÔNG chạy trên Swing EDT.
     * Nếu cần cập nhật UI phải dùng SwingUtilities.invokeLater().
     */
    void onMessageReceived(Message message);

    /** Gọi khi kết nối TCP tới Server thành công. */
    default void onConnected(String host, int port) {}

    /** Gọi khi mất kết nối với Server (timeout, lỗi mạng, disconnect chủ động). */
    default void onDisconnected(String reason) {}
}
