package tankops.common.net.codec;

import java.io.*;
import java.util.*;
import tankops.common.model.common.Direction;
import tankops.common.net.Message;
import tankops.common.net.lobby.*;
import tankops.common.net.game.*;
import tankops.common.net.system.*;

/**
 * Bộ mã hóa / giải mã giao thức nhị phân TankOps (Codec).
 *
 * ĐỊNH DẠNG KHUNG GÓI TIN (Wire Format):
 * ┌──────────────────┬─────────────────┬──────────────────────────────┐
 * │ Length (4 bytes) │ TypeID (1 byte) │ Payload ((Length-1) bytes)   │
 * └──────────────────┴─────────────────┴──────────────────────────────┘
 */
public final class MessageCodec {

    /** Giới hạn kích thước gói tin tối đa = 1 MB để phòng thủ DoS. */
    public static final int MAX_MESSAGE_SIZE = 1_048_576;

    private MessageCodec() { /* utility class */ }

    // =========================================================================
    // PUBLIC API: ENCODE
    // =========================================================================

    public static byte[] encode(Message message) throws ProtocolException {
        Objects.requireNonNull(message, "message cannot be null");
        try {
            ByteArrayOutputStream payloadBuf = new ByteArrayOutputStream(256);
            DataOutputStream payloadOut = new DataOutputStream(payloadBuf);
            writePayload(payloadOut, message);
            payloadOut.flush();
            byte[] payload = payloadBuf.toByteArray();

            int frameLength = 1 + payload.length;
            if (frameLength > MAX_MESSAGE_SIZE) {
                throw new ProtocolException(
                    "Kích thước gói tin (" + frameLength + " bytes) vượt giới hạn " + MAX_MESSAGE_SIZE
                );
            }

            ByteArrayOutputStream frameBuf = new ByteArrayOutputStream(4 + frameLength);
            DataOutputStream frameOut = new DataOutputStream(frameBuf);
            frameOut.writeInt(frameLength);
            frameOut.writeByte(message.getTypeId());
            frameOut.write(payload);
            frameOut.flush();
            return frameBuf.toByteArray();

        } catch (IOException e) {
            throw new ProtocolException("Lỗi khi mã hóa message: " + e.getMessage(), e);
        }
    }

    public static void writeMessage(DataOutputStream out, Message message)
            throws IOException, ProtocolException {
        byte[] frame = encode(message);
        out.write(frame);
        out.flush();
    }

    // =========================================================================
    // PUBLIC API: DECODE
    // =========================================================================

    public static Message decode(byte[] frameBytes) throws ProtocolException {
        Objects.requireNonNull(frameBytes, "frameBytes cannot be null");
        if (frameBytes.length < 5) {
            throw new ProtocolException(
                "Khung gói tin quá ngắn: " + frameBytes.length + " bytes (tối thiểu 5)"
            );
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(frameBytes))) {
            return readMessage(in);
        } catch (ProtocolException pe) {
            throw pe;
        } catch (IOException e) {
            throw new ProtocolException("Lỗi giải mã: " + e.getMessage(), e);
        }
    }

    public static Message readMessage(DataInputStream in) throws IOException, ProtocolException {
        int length;
        try {
            length = in.readInt();
        } catch (EOFException e) {
            throw e;
        } catch (IOException e) {
            throw new ProtocolException("Không đọc được tiền tố độ dài: " + e.getMessage(), e);
        }

        if (length < 1 || length > MAX_MESSAGE_SIZE) {
            throw new ProtocolException(
                "Độ dài gói tin không hợp lệ: " + length +
                " (hợp lệ: 1 - " + MAX_MESSAGE_SIZE + ")"
            );
        }

        byte typeId = in.readByte();
        int payloadLength = length - 1;
        byte[] payloadBytes = new byte[payloadLength];
        try {
            in.readFully(payloadBytes);
        } catch (EOFException e) {
            throw new ProtocolException(
                "Payload bị cắt ngắn đột ngột: mong đợi " + payloadLength + " bytes"
            );
        }

        try (DataInputStream payloadIn =
                new DataInputStream(new ByteArrayInputStream(payloadBytes))) {
            return readPayload(typeId, payloadIn);
        } catch (ProtocolException pe) {
            throw pe;
        } catch (Exception e) {
            throw new ProtocolException(
                "Lỗi phân tích payload cho Type 0x" +
                String.format("%02X", typeId & 0xFF) + ": " + e.getMessage(), e
            );
        }
    }

    // =========================================================================
    // WRITE PAYLOAD
    // =========================================================================
    private static void writePayload(DataOutputStream out, Message msg)
            throws IOException, ProtocolException {
        switch (msg) {
            case RoomStateMessage m -> {
                out.writeUTF(m.roomId() != null ? m.roomId() : "");

                // Ghi danh sách tên người chơi
                List<String> names = m.playerNames() != null ? m.playerNames() : List.of();
                out.writeInt(names.size());
                for (String name : names) {
                    out.writeUTF(name != null ? name : "Unknown");
                }

                out.writeUTF(m.hostId() != null ? m.hostId() : "");

                // Ghi danh sách người chơi đã Ready
                List<String> readyNames = m.readyPlayerNames() != null ? m.readyPlayerNames() : List.of();
                out.writeInt(readyNames.size());
                for (String readyName : readyNames) {
                    out.writeUTF(readyName != null ? readyName : "Unknown");
                }
            }
            case JoinRoomMessage m -> {
                out.writeUTF(m.roomId());
                out.writeUTF(m.playerName());
            }
            case JoinResponseMessage m -> {
                out.writeBoolean(m.success());
                out.writeUTF(m.message());
                out.writeUTF(m.roomId());
                out.writeUTF(m.assignedPlayerId());
                out.writeInt(m.playerNumber());
            }
            case LeaveRoomMessage m -> {
                out.writeUTF(m.roomId());
                out.writeUTF(m.playerId());
            }
            case RoomListRequestMessage m -> {
                out.writeUTF(m.requesterId());
            }
            case RoomListResponseMessage m -> {
                out.writeInt(m.rooms().size());
                for (RoomInfo r : m.rooms()) {
                    out.writeUTF(r.roomId());
                    out.writeInt(r.currentPlayers());
                    out.writeInt(r.maxPlayers());
                    out.writeUTF(r.state());
                }
            }
            case InputMessage m -> {
                out.writeUTF(m.playerId());
                out.writeInt(m.tick());
                writeDirection(out, m.direction());
                out.writeBoolean(m.shooting());
            }
            case SnapshotMessage m -> {
                out.writeInt(m.tick());
                out.writeBoolean(m.gameOver());
                out.writeUTF(m.winnerId() != null ? m.winnerId() : "");
                out.writeInt(m.tanks().size());
                for (TankSnapshot t : m.tanks()) {
                    out.writeUTF(t.id());
                    out.writeDouble(t.x());
                    out.writeDouble(t.y());
                    writeDirection(out, t.direction());
                    out.writeInt(t.health());
                    out.writeInt(t.maxHealth());
                    out.writeInt(t.starLevel());
                    out.writeBoolean(t.shielded());
                    out.writeBoolean(t.moving());
                    out.writeBoolean(t.destroyed());
                    out.writeInt(t.lives());
                    out.writeInt(t.score());
                }
                out.writeInt(m.bullets().size());
                for (BulletSnapshot b : m.bullets()) {
                    out.writeUTF(b.id());
                    out.writeUTF(b.ownerId());
                    out.writeDouble(b.x());
                    out.writeDouble(b.y());
                    writeDirection(out, b.direction());
                }
                // Vật phẩm đang nằm trên bản đồ
                out.writeInt(m.powerUps().size());
                for (PowerUpSnapshot p : m.powerUps()) {
                    out.writeUTF(p.id());
                    out.writeUTF(p.kind());
                    out.writeDouble(p.x());
                    out.writeDouble(p.y());
                }
                // Tường gạch/thép còn lại
                out.writeInt(m.environments().size());
                for (EnvironmentSnapshot e : m.environments()) {
                    out.writeUTF(e.id());
                    out.writeUTF(e.kind());
                    out.writeDouble(e.x());
                    out.writeDouble(e.y());
                }
            }
            case GameOverMessage m -> {
                out.writeUTF(m.winnerId() != null ? m.winnerId() : "");
                out.writeInt(m.scores().size());
                for (Map.Entry<String, Integer> e : m.scores().entrySet()) {
                    out.writeUTF(e.getKey());
                    out.writeInt(e.getValue());
                }
            }
            case EventMessage m -> {
                out.writeByte(m.eventType().ordinal());
                out.writeUTF(m.sourceId());
                out.writeUTF(m.targetId());
                out.writeDouble(m.x());
                out.writeDouble(m.y());
            }
            case ChatMessage m -> {
                out.writeUTF(m.sender());
                out.writeUTF(m.content());
            }
            case PingMessage m -> out.writeLong(m.timestamp());
            case PongMessage m -> out.writeLong(m.timestamp());
            case DisconnectMessage m -> {
                out.writeUTF(m.playerId());
                out.writeUTF(m.reason());
            }
            default -> throw new ProtocolException("Loại message không hỗ trợ: " + msg.getClass());
        }
    }

    // =========================================================================
    // READ PAYLOAD
    // =========================================================================
    private static Message readPayload(byte typeId, DataInputStream in)
            throws IOException, ProtocolException {
        return switch (typeId) {
            case RoomStateMessage.TYPE_ID -> {
                String rId = in.readUTF();
                int nameCount = in.readInt();
                List<String> names = new ArrayList<>(nameCount);
                for (int i = 0; i < nameCount; i++) {
                    names.add(in.readUTF());
                }
                String rawHost = in.readUTF();
                String hostId = rawHost.isEmpty() ? null : rawHost;
                // Đọc danh sách tên người chơi đã Ready
                int readyCount = in.readInt();
                List<String> readyNames = new ArrayList<>(readyCount);
                for (int i = 0; i < readyCount; i++) {
                    readyNames.add(in.readUTF());
                }
                yield new RoomStateMessage(rId, names, hostId, readyNames);
            }
            case JoinRoomMessage.TYPE_ID ->
                new JoinRoomMessage(in.readUTF(), in.readUTF());

            case JoinResponseMessage.TYPE_ID ->
                new JoinResponseMessage(
                    in.readBoolean(), in.readUTF(), in.readUTF(), in.readUTF(), in.readInt()
                );

            case LeaveRoomMessage.TYPE_ID ->
                new LeaveRoomMessage(in.readUTF(), in.readUTF());

            case RoomListRequestMessage.TYPE_ID ->
                new RoomListRequestMessage(in.readUTF());

            case RoomListResponseMessage.TYPE_ID -> {
                int count = in.readInt();
                List<RoomInfo> rooms = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    rooms.add(new RoomInfo(
                        in.readUTF(), in.readInt(), in.readInt(), in.readUTF()
                    ));
                }
                yield new RoomListResponseMessage(rooms);
            }

            case InputMessage.TYPE_ID ->
                new InputMessage(
                    in.readUTF(), in.readInt(), readDirection(in), in.readBoolean()
                );

            case SnapshotMessage.TYPE_ID -> {
                int tick      = in.readInt();
                boolean over  = in.readBoolean();
                String raw    = in.readUTF();
                String winner = raw.isEmpty() ? null : raw;

                int tankCount = in.readInt();
                List<TankSnapshot> tanks = new ArrayList<>(tankCount);
                for (int i = 0; i < tankCount; i++) {
                    tanks.add(new TankSnapshot(
                        in.readUTF(),
                        in.readDouble(), in.readDouble(),
                        readDirection(in),
                        in.readInt(), in.readInt(), in.readInt(),
                        in.readBoolean(), in.readBoolean(), in.readBoolean(),
                        in.readInt(), in.readInt()
                    ));
                }

                int bulletCount = in.readInt();
                List<BulletSnapshot> bullets = new ArrayList<>(bulletCount);
                for (int i = 0; i < bulletCount; i++) {
                    bullets.add(new BulletSnapshot(
                        in.readUTF(), in.readUTF(),
                        in.readDouble(), in.readDouble(),
                        readDirection(in)
                    ));
                }
                int powerUpCount = in.readInt();
                List<PowerUpSnapshot> powerUps = new ArrayList<>(powerUpCount);
                for (int i = 0; i < powerUpCount; i++) {
                    powerUps.add(new PowerUpSnapshot(
                        in.readUTF(), in.readUTF(), in.readDouble(), in.readDouble()
                    ));
                }

                int envCount = in.readInt();
                List<EnvironmentSnapshot> envs = new ArrayList<>(envCount);
                for (int i = 0; i < envCount; i++) {
                    envs.add(new EnvironmentSnapshot(
                        in.readUTF(), in.readUTF(), in.readDouble(), in.readDouble()
                    ));
                }
                yield new SnapshotMessage(tick, tanks, bullets, over, winner, powerUps, envs);
            }

            case GameOverMessage.TYPE_ID -> {
                String raw    = in.readUTF();
                String winner = raw.isEmpty() ? null : raw;
                int scoreCount = in.readInt();
                Map<String, Integer> scores = new LinkedHashMap<>(scoreCount);
                for (int i = 0; i < scoreCount; i++) {
                    scores.put(in.readUTF(), in.readInt());
                }
                yield new GameOverMessage(winner, scores);
            }

            case EventMessage.TYPE_ID -> {
                int ord = in.readByte() & 0xFF;
                EventMessage.EventType[] types = EventMessage.EventType.values();
                if (ord >= types.length) {
                    throw new ProtocolException("EventType ordinal không hợp lệ: " + ord);
                }
                yield new EventMessage(types[ord], in.readUTF(), in.readUTF(),
                    in.readDouble(), in.readDouble());
            }

            case ChatMessage.TYPE_ID ->
                new ChatMessage(in.readUTF(), in.readUTF());

            case PingMessage.TYPE_ID ->
                new PingMessage(in.readLong());

            case PongMessage.TYPE_ID ->
                new PongMessage(in.readLong());

            case DisconnectMessage.TYPE_ID ->
                new DisconnectMessage(in.readUTF(), in.readUTF());

            default -> throw new ProtocolException(
                "Type ID không được hỗ trợ: 0x" + String.format("%02X", typeId & 0xFF)
            );
        };
    }

    private static void writeDirection(DataOutputStream out, Direction dir) throws IOException {
        if (dir == null) {
            out.writeByte(0);
        } else {
            out.writeByte(dir.ordinal() + 1);
        }
    }

    private static Direction readDirection(DataInputStream in) throws IOException, ProtocolException {
        int b = in.readByte() & 0xFF;
        if (b == 0) return null;
        Direction[] values = Direction.values();
        if (b > values.length) {
            throw new ProtocolException("Direction ordinal không hợp lệ: " + b);
        }
        return values[b - 1];
    }
}

