package tankops.common.net.codec;

import java.io.IOException;

/**
 * Ngoại lệ ném ra khi gặp lỗi trong quá trình mã hóa / giải mã giao thức nhị phân.
 *
 * Kế thừa IOException để tích hợp tự nhiên với luồng I/O mạng TCP.
 */
public class ProtocolException extends IOException {

    public ProtocolException(String message) {
        super(message);
    }

    public ProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}

