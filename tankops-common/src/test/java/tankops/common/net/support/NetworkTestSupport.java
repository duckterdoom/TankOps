package tankops.common.net.support;

import tankops.common.net.Message;
import tankops.common.net.codec.MessageCodec;
import tankops.common.net.codec.ProtocolException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tiện ích hỗ trợ kiểm thử giao thức mạng TankOps (Test Fixture & Helpers).
 */
public final class NetworkTestSupport {

    private NetworkTestSupport() { /* utility class */ }

    /**
     * Thực hiện kiểm thử Round-Trip (Mã hóa -> Giải mã -> So sánh với bản gốc).
     *
     * @param original Bản tin gốc cần test
     * @param <T> Kiểu bản tin cụ thể
     * @return Bản tin sau khi giải mã
     */
    @SuppressWarnings("unchecked")
    public static <T extends Message> T assertRoundTrip(T original) throws ProtocolException {
        assertNotNull(original, "Original message must not be null");

        // Mã hóa ra mảng byte
        byte[] encoded = MessageCodec.encode(original);
        assertNotNull(encoded);

        // Giải mã ngược lại
        Message decoded = MessageCodec.decode(encoded);
        assertNotNull(decoded);

        // Kiểm tra đúng kiểu và đúng TypeID
        assertEquals(original.getClass(), decoded.getClass(), "Class type must match");
        assertEquals(original.getTypeId(), decoded.getTypeId(), "Type ID must match");
        assertEquals(original, decoded, "Decoded message must equal original");

        // Mã hóa lại lần 2 phải ra byte y hệt (idempotent)
        byte[] reEncoded = MessageCodec.encode(decoded);
        assertArrayEquals(encoded, reEncoded, "Re-encoded bytes must equal original encoded bytes");

        return (T) decoded;
    }
}
