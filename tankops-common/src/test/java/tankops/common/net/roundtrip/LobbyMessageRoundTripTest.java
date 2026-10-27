package tankops.common.net.roundtrip;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tankops.common.net.codec.ProtocolException;
import tankops.common.net.lobby.JoinResponseMessage;
import tankops.common.net.lobby.JoinRoomMessage;
import tankops.common.net.lobby.LeaveRoomMessage;
import tankops.common.net.lobby.RoomInfo;
import tankops.common.net.lobby.RoomListRequestMessage;
import tankops.common.net.lobby.RoomListResponseMessage;
import tankops.common.net.lobby.RoomStateMessage;
import tankops.common.net.support.NetworkTestSupport;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Kiểm thử Round-Trip: Nhóm Quản lý Phòng & Sảnh (Lobby Messages)")
class LobbyMessageRoundTripTest {

    @Test
    @DisplayName("0x01: JoinRoomMessage round-trip")
    void testJoinRoomMessage() throws ProtocolException {
        JoinRoomMessage original = new JoinRoomMessage("room-123", "PlayerPro");
        JoinRoomMessage decoded = NetworkTestSupport.assertRoundTrip(original);
        assertEquals("room-123", decoded.roomId());
        assertEquals("PlayerPro", decoded.playerName());
    }

    @Test
    @DisplayName("0x02: JoinResponseMessage round-trip")
    void testJoinResponseMessage() throws ProtocolException {
        JoinResponseMessage successMsg = new JoinResponseMessage(true, "Vào phòng thành công", "room-123", "player-1", 1);
        JoinResponseMessage decoded = NetworkTestSupport.assertRoundTrip(successMsg);
        assertTrue(decoded.success());
        assertEquals("room-123", decoded.roomId());
        assertEquals("player-1", decoded.assignedPlayerId());
        assertEquals(1, decoded.playerNumber());
    }

    @Test
    @DisplayName("0x03: LeaveRoomMessage round-trip")
    void testLeaveRoomMessage() throws ProtocolException {
        LeaveRoomMessage original = new LeaveRoomMessage("room-123", "player-1");
        LeaveRoomMessage decoded = NetworkTestSupport.assertRoundTrip(original);
        assertEquals("room-123", decoded.roomId());
        assertEquals("player-1", decoded.playerId());
    }

    @Test
    @DisplayName("0x04: RoomListRequestMessage round-trip")
    void testRoomListRequestMessage() throws ProtocolException {
        RoomListRequestMessage original = new RoomListRequestMessage("player-1");
        RoomListRequestMessage decoded = NetworkTestSupport.assertRoundTrip(original);
        assertEquals("player-1", decoded.requesterId());
    }

    @Test
    @DisplayName("0x05: RoomListResponseMessage round-trip (kèm danh sách RoomInfo)")
    void testRoomListResponseMessage() throws ProtocolException {
        List<RoomInfo> rooms = List.of(
                new RoomInfo("room-1", 2, 5, "LOBBY"),
                new RoomInfo("room-2", 4, 5, "PLAYING")
        );
        RoomListResponseMessage original = new RoomListResponseMessage(rooms);
        RoomListResponseMessage decoded = NetworkTestSupport.assertRoundTrip(original);
        assertEquals(2, decoded.rooms().size());
        assertEquals("room-1", decoded.rooms().get(0).roomId());
        assertEquals("PLAYING", decoded.rooms().get(1).state());
    }

    @Test
    @DisplayName("0x0E: RoomStateMessage round-trip (danh sách người chơi, chủ phòng, người READY)")
    void testRoomStateMessage() throws ProtocolException {
        RoomStateMessage original = new RoomStateMessage(
                "room-1", List.of("An", "Binh", "Chi"), "An", List.of("Binh"));
        RoomStateMessage decoded = NetworkTestSupport.assertRoundTrip(original);
        assertEquals("room-1", decoded.roomId());
        assertEquals(List.of("An", "Binh", "Chi"), decoded.playerNames());
        assertEquals("An", decoded.hostId());
        assertEquals(List.of("Binh"), decoded.readyPlayerNames());
    }

    @Test
    @DisplayName("0x0E: RoomStateMessage khi không có chủ phòng và không ai READY")
    void testRoomStateMessageEmptyReadyAndNoHost() throws ProtocolException {
        RoomStateMessage original = new RoomStateMessage("room-2", List.of(), null, List.of());
        RoomStateMessage decoded = NetworkTestSupport.assertRoundTrip(original);
        assertEquals(0, decoded.playerNames().size());
        assertEquals(null, decoded.hostId());
        assertEquals(0, decoded.readyPlayerNames().size());
    }
}
