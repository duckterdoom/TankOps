package tankops.server.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import tankops.server.game.GameRoom;

import org.junit.jupiter.api.Test;

import tankops.common.model.common.Direction;
import tankops.common.net.Message;
import tankops.common.net.codec.MessageCodec;
import tankops.common.net.game.InputMessage;
import tankops.common.net.game.SnapshotMessage;
import tankops.common.net.lobby.JoinResponseMessage;
import tankops.common.net.lobby.JoinRoomMessage;
import tankops.common.net.lobby.RoomInfo;
import tankops.common.net.lobby.RoomListRequestMessage;
import tankops.common.net.lobby.RoomListResponseMessage;
import tankops.common.net.system.ChatMessage;

// Test "đầu-cuối": server thật + client giả nói chuyện bằng đúng giao thức nhị phân của M1
class ServerEndToEndTest {

    // Client giả rất đơn giản: gửi tin, đọc tin
    private static class FakeClient {
        final Socket socket;
        final DataInputStream in;
        final DataOutputStream out;

        FakeClient(int port) throws Exception {
            socket = new Socket("localhost", port);
            socket.setSoTimeout(5000); // đọc quá 5 giây không có gì thì báo lỗi, test không treo
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        }

        void send(Message m) throws Exception {
            MessageCodec.writeMessage(out, m);
        }

        Message read() throws Exception {
            return MessageCodec.readMessage(in);
        }

        // Đọc cho tới khi gặp tin kiểu mong muốn (bỏ qua tin khác như snapshot)
        <T extends Message> T readUntil(Class<T> type) throws Exception {
            for (int i = 0; i < 500; i++) {
                Message m = read();
                if (type.isInstance(m)) {
                    return type.cast(m);
                }
            }
            throw new AssertionError("Không nhận được " + type.getSimpleName());
        }
    }

    @Test
    void lobby_join_start_snapshot_andShoot() throws Exception {
        GameServer server = new GameServer();
        ServerSocket ss = new ServerSocket(0, 500);
        new Thread(new ConnectionAcceptor(ss, server)).start();
        int port = ss.getLocalPort();
        FakeClient a = null;
        FakeClient b = null;
        try {
            a = new FakeClient(port);
            b = new FakeClient(port);

            // 1) Vừa vào lobby đã thấy 3 phòng chờ có sẵn
            a.send(new RoomListRequestMessage("a"));
            List<RoomInfo> rooms = a.readUntil(RoomListResponseMessage.class).rooms();
            assertEquals(3, rooms.size());
            assertEquals(5, rooms.get(0).maxPlayers());

            // 2) A vào room-1, B vào "auto" -> cùng phòng room-1
            a.send(new JoinRoomMessage("room-1", "A"));
            JoinResponseMessage ra = a.readUntil(JoinResponseMessage.class);
            assertTrue(ra.success());
            assertEquals("room-1", ra.roomId());

            b.send(new JoinRoomMessage("auto", "B"));
            JoinResponseMessage rb = b.readUntil(JoinResponseMessage.class);
            assertTrue(rb.success());
            assertEquals("room-1", rb.roomId());

            // 3) Phòng không tồn tại -> bị từ chối rõ ràng
            FakeClient c = new FakeClient(port);
            c.send(new JoinRoomMessage("room-99", "C"));
            assertTrue(!c.readUntil(JoinResponseMessage.class).success());
            c.socket.close();

            // 4) A gõ /start -> cả 2 nhận snapshot
            a.send(new ChatMessage("a", "/start"));
            SnapshotMessage sa = a.readUntil(SnapshotMessage.class);
            SnapshotMessage sb = b.readUntil(SnapshotMessage.class);
            assertEquals(2, sa.tanks().size());
            assertEquals(2, sb.tanks().size());

            // 5) Gửi input bắn: server không được sập, vẫn tiếp tục gửi snapshot
            a.send(new InputMessage(ra.assignedPlayerId(), 1, Direction.UP, true));
            SnapshotMessage later = a.readUntil(SnapshotMessage.class);
            assertTrue(later.tick() >= sa.tick());
        } finally {
            if (a != null) a.socket.close();
            if (b != null) b.socket.close();
            ss.close();
            server.getRoomManager().listRooms().forEach(GameRoom::stopGame);
        }
    }
}