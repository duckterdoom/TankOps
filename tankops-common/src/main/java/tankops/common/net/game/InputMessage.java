package tankops.common.net.game;

import tankops.common.model.common.Direction;
import tankops.common.net.Message;
import java.util.Objects;

/**
 * Client → Server: Gửi lệnh điều khiển xe tăng từ người chơi tại mỗi nhịp client.
 * Type ID: 0x06
 */
public record InputMessage(
        String playerId,
        int tick,
        Direction direction,
        boolean shooting
) implements Message {

    public static final byte TYPE_ID = 0x06;

    public InputMessage {
        Objects.requireNonNull(playerId, "playerId cannot be null");
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }
}

