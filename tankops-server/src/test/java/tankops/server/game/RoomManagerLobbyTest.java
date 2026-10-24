package tankops.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import tankops.common.map.GameMap;
import tankops.common.map.MapLoader;
import tankops.server.net.ClientSession;

// Test cho phần "phòng có sẵn + tạo phòng + vào phòng tự động + bắt đầu ván"
class RoomManagerLobbyTest {

    private ClientSession fakePlayer() {
        return new ClientSession(null, null);
    }

    private GameMap map() throws Exception {
        return new MapLoader().loadMap("/maps/map_multi.map");
    }

    private int countState(RoomManager m, RoomState state) {
        return (int) m.listRooms().stream().filter(r -> r.getState() == state).count();
    }

    @Test
    void defaultRooms_createdAtStart() {
        RoomManager m = new RoomManager(10, 5, 3);
        assertEquals(3, m.listRooms().size());
        assertEquals(3, countState(m, RoomState.LOBBY));
    }

    @Test
    void emptyDefaultRoom_isKept() throws Exception {
        RoomManager m = new RoomManager(10, 5, 3);
        ClientSession p = fakePlayer();
        GameRoom r = m.joinOrCreate("room-1", p);
        m.leaveRoom(r.getId(), p);
        assertEquals(3, m.listRooms().size());
    }

    @Test
    void joinAuto_fillsFirstWaitingRoom() throws Exception {
        RoomManager m = new RoomManager(10, 5, 3);
        GameRoom a = m.joinOrCreate("auto", fakePlayer());
        GameRoom b = m.joinOrCreate(null, fakePlayer());
        assertEquals("room-1", a.getId());
        assertEquals("room-1", b.getId());
        assertEquals(2, a.getPlayerCount());
    }

    @Test
    void joinNew_createsNewRoom() throws Exception {
        RoomManager m = new RoomManager(10, 5, 3);
        GameRoom r = m.joinOrCreate("new", fakePlayer());
        assertEquals("room-4", r.getId());
        assertEquals(4, m.listRooms().size());
    }

    @Test
    void joinUnknownRoom_refused_noRoomCreated() {
        RoomManager m = new RoomManager(10, 5, 3);
        assertThrows(IllegalArgumentException.class, () -> m.joinOrCreate("room-99", fakePlayer()));
        assertEquals(3, m.listRooms().size());
    }

    @Test
    void maxRoomsCap_doesNotCrash() throws Exception {
        RoomManager m = new RoomManager(3, 5, 3);
        assertThrows(IllegalStateException.class, () -> m.joinOrCreate("new", fakePlayer()));
        assertEquals(3, m.listRooms().size());
    }

    @Test
    void alreadyInRoom_refused() throws Exception {
        RoomManager m = new RoomManager(10, 5, 3);
        ClientSession p = fakePlayer();
        m.joinOrCreate("room-1", p);
        assertThrows(IllegalStateException.class, () -> m.joinOrCreate("room-2", p));
        assertEquals(1, m.getRoom("room-1").getPlayerCount());
        assertEquals(0, m.getRoom("room-2").getPlayerCount());
    }

    @Test
    void startGame_nonMemberRefused() throws Exception {
        RoomManager m = new RoomManager(10, 5, 3);
        m.joinOrCreate("room-1", fakePlayer());
        m.joinOrCreate("room-1", fakePlayer());
        ClientSession outsider = fakePlayer();
        assertThrows(IllegalStateException.class, () -> m.startGame("room-1", outsider, map(), 30));
        assertEquals(RoomState.LOBBY, m.getRoom("room-1").getState());
    }

    @Test
    void startGame_onePlayerRefused_twoPlayersOk_andReplacementRoomAppears() throws Exception {
        RoomManager m = new RoomManager(10, 5, 3);
        ClientSession p1 = fakePlayer();
        ClientSession p2 = fakePlayer();
        m.joinOrCreate("room-1", p1);
        assertThrows(IllegalStateException.class, () -> m.startGame("room-1", p1, map(), 30));

        m.joinOrCreate("room-1", p2);
        m.startGame("room-1", p2, map(), 30);
        try {
            assertEquals(RoomState.PLAYING, m.getRoom("room-1").getState());
            // phòng đang chơi không còn là phòng chờ -> server tạo thêm phòng chờ mới
            assertEquals(3, countState(m, RoomState.LOBBY));
            assertEquals(4, m.listRooms().size());
        } finally {
            m.getRoom("room-1").stopGame();
        }
    }

    @Test
    void playingRoom_thatEmpties_isRemoved() throws Exception {
        RoomManager m = new RoomManager(10, 5, 3);
        ClientSession p1 = fakePlayer();
        ClientSession p2 = fakePlayer();
        m.joinOrCreate("room-1", p1);
        m.joinOrCreate("room-1", p2);
        m.startGame("room-1", p1, map(), 30);
        m.leaveRoom("room-1", p1);
        m.leaveRoom("room-1", p2);
        assertEquals(null, m.getRoom("room-1"));
        assertEquals(3, m.listRooms().size());
    }

    // 50 thread cùng "auto": không phòng nào vượt sức chứa, tổng người = số lần thành công
    @Test
    void concurrentAutoJoin_neverExceedsCapacity() throws Exception {
        for (int round = 0; round < 30; round++) {
            RoomManager m = new RoomManager(10, 5, 3);
            int clients = 50;
            CountDownLatch go = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(clients);
            AtomicInteger ok = new AtomicInteger();
            for (int i = 0; i < clients; i++) {
                new Thread(() -> {
                    try {
                        go.await();
                        m.joinOrCreate("auto", fakePlayer());
                        ok.incrementAndGet();
                    } catch (Exception ignored) {
                        // phòng đầy / hết phòng: chấp nhận
                    } finally {
                        done.countDown();
                    }
                }).start();
            }
            go.countDown();
            assertTrue(done.await(10, java.util.concurrent.TimeUnit.SECONDS));

            int total = 0;
            for (GameRoom r : m.listRooms()) {
                assertTrue(r.getPlayerCount() <= 5);
                total += r.getPlayerCount();
            }
            assertEquals(ok.get(), total);
            assertEquals(50, ok.get()); // 10 phòng x 5 chỗ = vừa đủ 50
        }
    }

    // Vào/ra liên tục: không để lại phòng "ma" và luôn còn >= 3 phòng chờ
    @Test
    void joinLeaveLoops_leaveNoZombieRooms() throws Exception {
        RoomManager m = new RoomManager(10, 5, 3);
        int threads = 20;
        CountDownLatch done = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < 200; i++) {
                        ClientSession p = fakePlayer();
                        try {
                            GameRoom r = m.joinOrCreate(i % 3 == 0 ? "new" : "auto", p);
                            m.leaveRoom(r.getId(), p);
                        } catch (Exception ignored) {
                            // hết phòng/đầy: bỏ qua
                        }
                    }
                } finally {
                    done.countDown();
                }
            }).start();
        }
        assertTrue(done.await(20, java.util.concurrent.TimeUnit.SECONDS));
        List<GameRoom> rooms = m.listRooms();
        assertEquals(3, rooms.size());
        for (GameRoom r : rooms) {
            assertEquals(0, r.getPlayerCount());
            assertEquals(RoomState.LOBBY, r.getState());
        }
    }
}