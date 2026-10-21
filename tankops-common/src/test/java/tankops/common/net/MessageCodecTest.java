package tankops.common.net;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tankops.common.model.common.Direction;
import tankops.common.net.codec.*;
import tankops.common.net.lobby.*;
import tankops.common.net.game.*;
import tankops.common.net.system.*;

/**
 * Kiểm thử bộ mã hóa/giải mã giao thức mạng TankOps.
 *
 * TC06: "Encode then decode each message type → Decoded message equals original"
 * TC07: "Random/invalid bytes sent → Server rejects it; no crash"
 */
class MessageCodecTest {

    // =========================================================================
    // HELPER
    // =========================================================================
    private Message roundTrip(Message original) throws Exception {
        byte[] encoded = MessageCodec.encode(original);
        return MessageCodec.decode(encoded);
    }

    // =========================================================================
    // TC06: ROUND-TRIP TEST CHO TẤT CẢ CÁC LOẠI BẢN TIN
    // =========================================================================
    @Nested
    @DisplayName("TC06: Round-Trip Tests (Mã hóa -> Giải mã -> So khớp)")
    class TC06RoundTrip {

        @Test
        @DisplayName("0x01: JoinRoomMessage round-trip")
        void testJoinRoomMessage() throws Exception {
            JoinRoomMessage msg = new JoinRoomMessage("room-1", "Player1");
            Message result = roundTrip(msg);
            assertInstanceOf(JoinRoomMessage.class, result);
            JoinRoomMessage cast = (JoinRoomMessage) result;
            assertEquals("room-1", cast.roomId());
            assertEquals("Player1", cast.playerName());
            assertEquals(msg, result);
        }

        @Test
        @DisplayName("0x02: JoinResponseMessage round-trip (thành công)")
        void testJoinResponseMessage_success() throws Exception {
            JoinResponseMessage msg = new JoinResponseMessage(
                true, "Vào phòng thành công", "room-1", "player-1", 1
            );
            Message result = roundTrip(msg);
            assertInstanceOf(JoinResponseMessage.class, result);
            JoinResponseMessage cast = (JoinResponseMessage) result;
            assertTrue(cast.success());
            assertEquals("Vào phòng thành công", cast.message());
            assertEquals("room-1", cast.roomId());
            assertEquals("player-1", cast.assignedPlayerId());
            assertEquals(1, cast.playerNumber());
            assertEquals(msg, result);
        }

        @Test
        @DisplayName("0x02: JoinResponseMessage round-trip (thất bại, phòng đầy)")
        void testJoinResponseMessage_failure() throws Exception {
            JoinResponseMessage msg = new JoinResponseMessage(
                false, "Phòng đã đầy (5/5)", "room-1", "", -1
            );
            Message result = roundTrip(msg);
            assertInstanceOf(JoinResponseMessage.class, result);
            assertFalse(((JoinResponseMessage) result).success());
            assertEquals(msg, result);
        }

        @Test
        @DisplayName("0x03: LeaveRoomMessage round-trip")
        void testLeaveRoomMessage() throws Exception {
            LeaveRoomMessage msg = new LeaveRoomMessage("room-1", "player-1");
            Message result = roundTrip(msg);
            assertInstanceOf(LeaveRoomMessage.class, result);
            assertEquals(msg, result);
        }

        @Test
        @DisplayName("0x04: RoomListRequestMessage round-trip")
        void testRoomListRequestMessage() throws Exception {
            RoomListRequestMessage msg = new RoomListRequestMessage("player-1");
            Message result = roundTrip(msg);
            assertInstanceOf(RoomListRequestMessage.class, result);
            assertEquals("player-1", ((RoomListRequestMessage) result).requesterId());
            assertEquals(msg, result);
        }

        @Test
        @DisplayName("0x05: RoomListResponseMessage round-trip (danh sách có phòng)")
        void testRoomListResponseMessage_withRooms() throws Exception {
            List<RoomInfo> rooms = List.of(
                new RoomInfo("room-1", 2, 5, "LOBBY"),
                new RoomInfo("room-2", 5, 5, "PLAYING"),
                new RoomInfo("room-3", 1, 5, "FINISHED")
            );
            RoomListResponseMessage msg = new RoomListResponseMessage(rooms);
            Message result = roundTrip(msg);
            assertInstanceOf(RoomListResponseMessage.class, result);
            RoomListResponseMessage cast = (RoomListResponseMessage) result;
            assertEquals(3, cast.rooms().size());
            assertEquals("room-1", cast.rooms().get(0).roomId());
            assertEquals(2, cast.rooms().get(0).currentPlayers());
            assertEquals("PLAYING", cast.rooms().get(1).state());
            assertEquals(msg, result);
        }

        @Test
        @DisplayName("0x05: RoomListResponseMessage round-trip (danh sách rỗng)")
        void testRoomListResponseMessage_empty() throws Exception {
            RoomListResponseMessage msg = new RoomListResponseMessage(List.of());
            Message result = roundTrip(msg);
            assertInstanceOf(RoomListResponseMessage.class, result);
            assertTrue(((RoomListResponseMessage) result).rooms().isEmpty());
            assertEquals(msg, result);
        }

        @Test
        @DisplayName("0x06: InputMessage round-trip (di chuyển và bắn)")
        void testInputMessage_moveAndShoot() throws Exception {
            InputMessage msg = new InputMessage("player-1", 42, Direction.UP, true);
            Message result = roundTrip(msg);
            assertInstanceOf(InputMessage.class, result);
            InputMessage cast = (InputMessage) result;
            assertEquals("player-1", cast.playerId());
            assertEquals(42, cast.tick());
            assertEquals(Direction.UP, cast.direction());
            assertTrue(cast.shooting());
            assertEquals(msg, result);
        }

        @Test
        @DisplayName("0x06: InputMessage round-trip (đứng yên direction=null, không bắn)")
        void testInputMessage_idle() throws Exception {
            InputMessage msg = new InputMessage("player-2", 100, null, false);
            Message result = roundTrip(msg);
            assertInstanceOf(InputMessage.class, result);
            InputMessage cast = (InputMessage) result;
            assertNull(cast.direction(), "direction phải null khi xe đứng yên");
            assertFalse(cast.shooting());
            assertEquals(msg, result);
        }

        @Test
        @DisplayName("0x06: InputMessage round-trip (tất cả 4 hướng)")
        void testInputMessage_allDirections() throws Exception {
            for (Direction dir : Direction.values()) {
                InputMessage msg = new InputMessage("p1", 1, dir, false);
                Message result = roundTrip(msg);
                assertEquals(dir, ((InputMessage) result).direction());
            }
        }

        @Test
        @DisplayName("0x07: SnapshotMessage round-trip (đầy đủ xe và đạn)")
        void testSnapshotMessage_full() throws Exception {
            List<TankSnapshot> tanks = List.of(
                new TankSnapshot("p1", 10.5, 20.0, Direction.UP, 1, 1, 2, true, true, false),
                new TankSnapshot("p2", 200.0, 150.0, Direction.DOWN, 0, 1, 0, false, false, true),
                new TankSnapshot("bot-1", 50.0, 50.0, null, 1, 1, 0, false, false, false)
            );
            List<BulletSnapshot> bullets = List.of(
                new BulletSnapshot("b1", "p1", 15.0, 25.0, Direction.UP),
                new BulletSnapshot("b2", "bot-1", 52.0, 60.0, Direction.RIGHT)
            );
            SnapshotMessage msg = new SnapshotMessage(120, tanks, bullets, false, null);

            Message result = roundTrip(msg);
            assertInstanceOf(SnapshotMessage.class, result);
            SnapshotMessage cast = (SnapshotMessage) result;
            assertEquals(120, cast.tick());
            assertFalse(cast.gameOver());
            assertNull(cast.winnerId());
            assertEquals(3, cast.tanks().size());
            assertEquals(2, cast.bullets().size());

            // Kiểm tra chi tiết xe tăng 1
            TankSnapshot t1 = cast.tanks().get(0);
            assertEquals("p1", t1.id());
            assertEquals(10.5, t1.x(), 0.001);
            assertEquals(20.0, t1.y(), 0.001);
            assertEquals(Direction.UP, t1.direction());
            assertEquals(2, t1.starLevel());
            assertTrue(t1.shielded());
            assertTrue(t1.moving());
            assertFalse(t1.destroyed());

            // Kiểm tra chi tiết đạn 1
            BulletSnapshot b1 = cast.bullets().get(0);
            assertEquals("b1", b1.id());
            assertEquals("p1", b1.ownerId());
            assertEquals(15.0, b1.x(), 0.001);
            assertEquals(Direction.UP, b1.direction());

            assertEquals(msg, result);
        }

        @Test
        @DisplayName("0x07: SnapshotMessage round-trip (rỗng và kết thúc trận)")
        void testSnapshotMessage_emptyAndOver() throws Exception {
            SnapshotMessage msg = new SnapshotMessage(300, List.of(), List.of(), true, "player-1");
            Message result = roundTrip(msg);
            assertInstanceOf(SnapshotMessage.class, result);
            SnapshotMessage cast = (SnapshotMessage) result;
            assertTrue(cast.gameOver());
            assertEquals("player-1", cast.winnerId());
            assertTrue(cast.tanks().isEmpty());
            assertTrue(cast.bullets().isEmpty());
            assertEquals(msg, result);
        }

        @Test
        @DisplayName("0x08: GameOverMessage round-trip (có người thắng)")
        void testGameOverMessage_winner() throws Exception {
            Map<String, Integer> scores = Map.of("player-1", 300, "player-2", 100);
            GameOverMessage msg = new GameOverMessage("player-1", scores);
            Message result = roundTrip(msg);
            assertInstanceOf(GameOverMessage.class, result);
            GameOverMessage cast = (GameOverMessage) result;
            assertEquals("player-1", cast.winnerId());
            assertEquals(300, cast.scores().get("player-1"));
            assertEquals(100, cast.scores().get("player-2"));
            assertEquals(msg, result);
        }

        @Test
        @DisplayName("0x08: GameOverMessage round-trip (hòa - winner=null)")
        void testGameOverMessage_draw() throws Exception {
            GameOverMessage msg = new GameOverMessage(null, Map.of("p1", 0, "p2", 0));
            Message result = roundTrip(msg);
            assertInstanceOf(GameOverMessage.class, result);
            assertNull(((GameOverMessage) result).winnerId());
            assertEquals(msg, result);
        }

        @Test
        @DisplayName("0x09: EventMessage round-trip (tất cả các EventType)")
        void testEventMessage_allTypes() throws Exception {
            for (EventMessage.EventType type : EventMessage.EventType.values()) {
                EventMessage msg = new EventMessage(type, "src-1", "tgt-1", 100.5, 200.5);
                Message result = roundTrip(msg);
                assertInstanceOf(EventMessage.class, result);
                EventMessage cast = (EventMessage) result;
                assertEquals(type, cast.eventType());
                assertEquals("src-1", cast.sourceId());
                assertEquals("tgt-1", cast.targetId());
                assertEquals(100.5, cast.x(), 0.001);
                assertEquals(200.5, cast.y(), 0.001);
                assertEquals(msg, result);
            }
        }

        @Test
        @DisplayName("0x0A: ChatMessage round-trip (hỗ trợ tiếng Việt UTF-8)")
        void testChatMessage_utf8() throws Exception {
            ChatMessage msg = new ChatMessage("Player1", "Xin chào! Bắn xe tăng rất vui 🎯");
            Message result = roundTrip(msg);
            assertInstanceOf(ChatMessage.class, result);
            ChatMessage cast = (ChatMessage) result;
            assertEquals("Player1", cast.sender());
            assertEquals("Xin chào! Bắn xe tăng rất vui 🎯", cast.content());
            assertEquals(msg, result);
        }

        @Test
        @DisplayName("0x0B: PingMessage round-trip")
        void testPingMessage() throws Exception {
            long now = System.currentTimeMillis();
            PingMessage msg = new PingMessage(now);
            Message result = roundTrip(msg);
            assertInstanceOf(PingMessage.class, result);
            assertEquals(now, ((PingMessage) result).timestamp());
            assertEquals(msg, result);
        }

        @Test
        @DisplayName("0x0C: PongMessage round-trip")
        void testPongMessage() throws Exception {
            long now = System.currentTimeMillis();
            PongMessage msg = new PongMessage(now);
            Message result = roundTrip(msg);
            assertInstanceOf(PongMessage.class, result);
            assertEquals(now, ((PongMessage) result).timestamp());
            assertEquals(msg, result);
        }

        @Test
        @DisplayName("0x0D: DisconnectMessage round-trip")
        void testDisconnectMessage() throws Exception {
            DisconnectMessage msg = new DisconnectMessage("player-1", "CLIENT_QUIT");
            Message result = roundTrip(msg);
            assertInstanceOf(DisconnectMessage.class, result);
            DisconnectMessage cast = (DisconnectMessage) result;
            assertEquals("player-1", cast.playerId());
            assertEquals("CLIENT_QUIT", cast.reason());
            assertEquals(msg, result);
        }
    }

    // =========================================================================
    // TC07: MALFORMED MESSAGE REJECTION (Phòng thủ dữ liệu rác, không crash)
    // =========================================================================
    @Nested
    @DisplayName("TC07: Malformed Message Rejection Tests (Từ chối dữ liệu rác)")
    class TC07Malformed {

        @Test
        @DisplayName("Mảng byte rỗng -> Ném ProtocolException")
        void testEmptyBytes_throwsProtocolException() {
            assertThrows(ProtocolException.class, () -> MessageCodec.decode(new byte[0]));
        }

        @Test
        @DisplayName("Khung ngắn hơn 5 bytes -> Ném ProtocolException")
        void testTooShortFrame_throwsProtocolException() {
            assertThrows(ProtocolException.class, () -> MessageCodec.decode(new byte[]{0, 0, 0, 1}));
        }

        @Test
        @DisplayName("Tiền tố độ dài âm -> Ném ProtocolException")
        void testNegativeLength_throwsProtocolException() throws Exception {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            out.writeInt(-5);
            out.writeByte(0x01);
            out.flush();
            assertThrows(ProtocolException.class, () -> MessageCodec.decode(buf.toByteArray()));
        }

        @Test
        @DisplayName("Tiền tố độ dài = 0 -> Ném ProtocolException")
        void testZeroLength_throwsProtocolException() throws Exception {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            out.writeInt(0);
            out.flush();
            assertThrows(ProtocolException.class, () -> MessageCodec.decode(buf.toByteArray()));
        }

        @Test
        @DisplayName("Tiền tố độ dài vượt quá 1MB (chống DoS) -> Ném ProtocolException")
        void testOversizedLength_throwsProtocolException() throws Exception {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            out.writeInt(MessageCodec.MAX_MESSAGE_SIZE + 1);
            out.writeByte(0x01);
            out.flush();
            assertThrows(ProtocolException.class, () -> MessageCodec.decode(buf.toByteArray()));
        }

        @Test
        @DisplayName("Type ID không tồn tại (0xFF) -> Ném ProtocolException không crash")
        void testUnknownTypeId_throwsProtocolException() throws Exception {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            out.writeInt(2); // length = 2 (1 byte type + 1 byte payload rác)
            out.writeByte((byte) 0xFF); // type lạ
            out.writeByte(0x00);
            out.flush();
            ProtocolException pe = assertThrows(
                ProtocolException.class,
                () -> MessageCodec.decode(buf.toByteArray())
            );
            assertTrue(pe.getMessage().contains("0xFF"), "Thông báo lỗi phải chứa Type ID lạ");
        }

        @Test
        @DisplayName("Payload bị cắt ngắn đột ngột -> Ném ProtocolException")
        void testTruncatedPayload_throwsProtocolException() throws Exception {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            out.writeInt(100); // Khai báo length = 100 bytes
            out.writeByte(0x01); // JoinRoomMessage
            out.writeUTF("room-1"); // Chỉ ghi một phần, thiếu playerName
            out.flush();
            assertThrows(ProtocolException.class, () -> MessageCodec.decode(buf.toByteArray()));
        }

        @Test
        @DisplayName("Dữ liệu ngẫu nhiên (fuzzing 100 mẫu ngẫu nhiên) -> Đều ném ProtocolException, không crash")
        void testRandomBytes_neverCrashes() {
            java.util.Random rnd = new java.util.Random(12345);
            for (int i = 0; i < 100; i++) {
                int len = rnd.nextInt(200);
                byte[] garbage = new byte[len];
                rnd.nextBytes(garbage);
                // Mọi mảng byte ngẫu nhiên phải ném ProtocolException hoặc EOFException, KHÔNG ĐƯỢC crash
                assertThrows(Exception.class, () -> MessageCodec.decode(garbage));
            }
        }
    }

    // =========================================================================
    // STREAM I/O TESTS
    // =========================================================================
    @Nested
    @DisplayName("Stream I/O Tests (writeMessage và readMessage)")
    class StreamIO {

        @Test
        @DisplayName("Ghi và đọc trực tiếp từ DataStream thành công")
        void testWriteAndReadMessage() throws Exception {
            ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outBuf);

            JoinRoomMessage original = new JoinRoomMessage("room-99", "Tester");
            MessageCodec.writeMessage(out, original);

            ByteArrayInputStream inBuf = new ByteArrayInputStream(outBuf.toByteArray());
            DataInputStream in = new DataInputStream(inBuf);

            Message received = MessageCodec.readMessage(in);
            assertEquals(original, received);
        }

        @Test
        @DisplayName("Ghi và đọc liên tiếp nhiều message trên cùng stream (pipelining)")
        void testMultipleMessagesOnSameStream() throws Exception {
            ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(outBuf);

            Message m1 = new PingMessage(1000L);
            Message m2 = new InputMessage("p1", 1, Direction.UP, false);
            Message m3 = new ChatMessage("p1", "hello");

            MessageCodec.writeMessage(out, m1);
            MessageCodec.writeMessage(out, m2);
            MessageCodec.writeMessage(out, m3);

            ByteArrayInputStream inBuf = new ByteArrayInputStream(outBuf.toByteArray());
            DataInputStream in = new DataInputStream(inBuf);

            assertEquals(m1, MessageCodec.readMessage(in));
            assertEquals(m2, MessageCodec.readMessage(in));
            assertEquals(m3, MessageCodec.readMessage(in));
        }
    }
}
