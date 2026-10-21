package tankops.common.net.system;

import tankops.common.net.Message;

/**
 * Client ⇄ Server: Kiểm tra độ trễ (RTT latency) và duy trì kết nối (Heartbeat).
 * Type ID: 0x0B
 */
public record PingMessage(
        long timestamp
) implements Message {

    public static final byte TYPE_ID = 0x0B;

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }
}

