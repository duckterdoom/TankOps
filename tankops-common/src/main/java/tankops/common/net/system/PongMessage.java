package tankops.common.net.system;

import tankops.common.net.Message;

/**
 * Client ⇄ Server: Phản hồi bản tin PingMessage.
 * Type ID: 0x0C
 */
public record PongMessage(
        long timestamp
) implements Message {

    public static final byte TYPE_ID = 0x0C;

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }
}

