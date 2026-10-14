package tankops.server.game;

/**
 * Lỗi báo "phòng đã đầy" (test TC09).
 * Extends Exception nên là lỗi "checked": nơi nào gọi joinRoom bắt buộc phải xử lý nó.
 */
public class RoomFullException extends Exception {

    public RoomFullException(String message) {
        super(message);
    }
}