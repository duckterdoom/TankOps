package tankops.common.net.system;

import tankops.common.net.Message;
import java.util.Objects;

/**
 * Client ⇄ Server: Thông báo ngắt kết nối an toàn kèm lý do.
 * Type ID: 0x0D
 */
public record DisconnectMessage(
        String playerId,
        String reason
) implements Message {

    public static final byte TYPE_ID = 0x0D;

    public DisconnectMessage {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }
}

