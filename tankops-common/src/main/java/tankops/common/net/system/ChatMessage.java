package tankops.common.net.system;

import tankops.common.net.Message;
import java.util.Objects;

/**
 * Client ⇄ Server: Tin nhắn trò chuyện giữa người chơi trong phòng.
 * Type ID: 0x0A
 */
public record ChatMessage(
        String sender,
        String content
) implements Message {

    public static final byte TYPE_ID = 0x0A;

    public ChatMessage {
        Objects.requireNonNull(sender, "sender cannot be null");
        Objects.requireNonNull(content, "content cannot be null");
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }
}

