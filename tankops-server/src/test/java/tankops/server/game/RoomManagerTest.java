package tankops.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import tankops.server.net.ClientSession;

class RoomManagerTest {

    // "Người chơi giả": ClientSession chưa chạy nên không cần socket thật
    private ClientSession fakePlayer() {
        return new ClientSession(null, null);
    }

    // TC09: phòng 5 người, người thứ 6 bị từ chối
    @Test
    void tc09_roomFull_sixthPlayerRefused() throws Exception {
        RoomManager manager = new RoomManager(10, 5);
        GameRoom room = manager.createRoom();

        for (int i = 0; i < 5; i++) {
            manager.joinRoom(room.getId(), fakePlayer());
        }

        assertThrows(RoomFullException.class,
                () -> manager.joinRoom(room.getId(), fakePlayer()));
        assertEquals(5, room.getPlayerCount());
    }

    // TC10: 50 người cùng vào phòng chỉ có 1 chỗ, đúng 1 người thành công.
    // Lỗi tranh chấp (race) chỉ xảy ra thỉnh thoảng nên ta thử lặp lại 100 vòng cho chắc.
    @Test
    void tc10_concurrentJoin_exactlyOneSucceeds() throws Exception {
        for (int round = 0; round < 100; round++) {
            RoomManager manager = new RoomManager(10, 1);
            GameRoom room = manager.createRoom();

            int clients = 50;
            CountDownLatch startSignal = new CountDownLatch(1); // cho tất cả thread xuất phát cùng lúc
            CountDownLatch allDone = new CountDownLatch(clients);
            AtomicInteger success = new AtomicInteger();
            AtomicInteger refused = new AtomicInteger();

            for (int i = 0; i < clients; i++) {
                new Thread(() -> {
                    try {
                        startSignal.await();
                        manager.joinRoom(room.getId(), fakePlayer());
                        success.incrementAndGet();
                    } catch (RoomFullException e) {
                        refused.incrementAndGet();
                    } catch (InterruptedException e) {
                        // bỏ qua
                    } finally {
                        allDone.countDown();
                    }
                }).start();
            }

            startSignal.countDown(); // bắn súng lệnh: cả 50 thread cùng chạy
            allDone.await();         // chờ cả 50 thread xong

            assertEquals(1, success.get());
            assertEquals(clients - 1, refused.get());
            assertEquals(1, room.getPlayerCount());
        }
    }

    // Vượt số phòng tối đa thì bị từ chối
    @Test
    void createRoom_overLimit_throws() {
        RoomManager manager = new RoomManager(2, 5);
        manager.createRoom();
        manager.createRoom();
        assertThrows(IllegalStateException.class, manager::createRoom);
    }

    // Người cuối rời đi thì phòng bị xóa
    @Test
    void leaveRoom_lastPlayerLeaves_roomRemoved() throws Exception {
        RoomManager manager = new RoomManager(10, 5);
        GameRoom room = manager.createRoom();
        ClientSession player = fakePlayer();

        manager.joinRoom(room.getId(), player);
        assertEquals(1, manager.listRooms().size());

        manager.leaveRoom(room.getId(), player);
        assertEquals(0, manager.listRooms().size());
    }
}