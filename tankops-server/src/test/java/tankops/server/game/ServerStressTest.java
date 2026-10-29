package tankops.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import tankops.common.map.GameMap;
import tankops.common.map.MapLoader;
import tankops.server.net.ClientSession;
import tankops.server.net.ConnectionAcceptor;
import tankops.server.net.GameServer;

/**
 * T27: test áp lực (stress test) tìm lỗi tranh chấp giữa các thread.
 * Cách làm chung: cho thật nhiều thread cùng làm việc một lúc, rồi kiểm tra các "điều luôn phải đúng".
 */
class ServerStressTest {

    // Từ khi có READY: mọi người trừ chủ phòng (người đầu tiên) phải READY thì mới START được
    private static void ready(GameRoom room) {
        java.util.List<tankops.server.net.ClientSession> ps = room.getPlayers();
        for (int i = 1; i < ps.size(); i++) {
            room.setPlayerReady(ps.get(i).getPlayerName(), true);
        }
    }


    private GameMap map() {
        return new MapLoader().loadMap("/maps/map_multi.map");
    }

    private ClientSession fakePlayer() {
        return new ClientSession(null, null);
    }

    private void waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 300 && !condition.getAsBoolean(); i++) {
            Thread.sleep(10);
        }
    }

    // Chờ các thread xong tối đa 30 giây. Quá hạn => nghi bị đứng máy (deadlock) nên báo rõ.
    private void joinAllOrFail(List<Thread> threads) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30_000;
        for (Thread t : threads) {
            t.join(Math.max(1, deadline - System.currentTimeMillis()));
        }
        boolean stuck = false;
        for (Thread t : threads) {
            if (t.isAlive()) {
                stuck = true;
            }
        }
        assertNull(ManagementFactory.getThreadMXBean().findDeadlockedThreads()); // có deadlock thì fail
        assertTrue(!stuck); // còn thread chưa xong cũng fail
    }

    // ---------------------------------------------------------------------------------
    // 1. Nhiều thread cùng tạo phòng, vào phòng, rời phòng: không phòng nào quá người,
    //    không có lỗi lạ, không đứng máy.
    // ---------------------------------------------------------------------------------
    @Test
    void manyThreads_createJoinLeave_neverOverCapacity_noDeadlock() throws Exception {
        final int maxPlayers = 5;
        RoomManager manager = new RoomManager(10, maxPlayers);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
        AtomicInteger overCapacity = new AtomicInteger();
        CountDownLatch go = new CountDownLatch(1);

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            final int seed = i;
            Thread t = new Thread(() -> {
                Random random = new Random(seed);
                List<ClientSession> myPlayers = new ArrayList<>();
                List<String> myRooms = new ArrayList<>();
                try {
                    go.await();
                    for (int k = 0; k < 2000; k++) {
                        int action = random.nextInt(4);
                        if (action == 0) {
                            try {
                                manager.createRoom();
                            } catch (IllegalStateException e) {
                                // đã đủ số phòng: bình thường
                            }
                        } else if (action == 1) {
                            List<GameRoom> rooms = manager.listRooms();
                            if (!rooms.isEmpty()) {
                                GameRoom room = rooms.get(random.nextInt(rooms.size()));
                                ClientSession p = fakePlayer();
                                try {
                                    manager.joinRoom(room.getId(), p);
                                    myPlayers.add(p);
                                    myRooms.add(room.getId());
                                } catch (RoomFullException | IllegalArgumentException e) {
                                    // phòng đầy hoặc phòng vừa bị xóa: bình thường
                                }
                            }
                        } else if (action == 2) {
                            if (!myPlayers.isEmpty()) {
                                int index = random.nextInt(myPlayers.size());
                                manager.leaveRoom(myRooms.remove(index), myPlayers.remove(index));
                            }
                        } else {
                            for (GameRoom room : manager.listRooms()) {
                                if (room.getPlayerCount() > maxPlayers) {
                                    overCapacity.incrementAndGet(); // phòng quá người: LỖI
                                }
                            }
                        }
                    }
                } catch (Throwable e) {
                    errors.add(e);
                }
            });
            threads.add(t);
            t.start();
        }
        go.countDown();
        joinAllOrFail(threads);

        assertEquals(0, errors.size());
        assertEquals(0, overCapacity.get());
        for (GameRoom room : manager.listRooms()) {
            assertTrue(room.getPlayerCount() <= maxPlayers);
        }
    }

    // ---------------------------------------------------------------------------------
    // 2. "Vào phòng" đua với "bắt đầu ván": không ai được lọt vào sau khi ván đã bắt đầu.
    //    Điều luôn đúng: số người trong phòng == số người có tên trong bảng điểm.
    // ---------------------------------------------------------------------------------
    @Test
    void joinRacesWithStart_nobodySneaksIn() throws Exception {
        for (int round = 0; round < 100; round++) {
            GameRoom room = new GameRoom("room-1", 50);
            room.addPlayer(fakePlayer());
            room.addPlayer(fakePlayer());

            CountDownLatch go = new CountDownLatch(1);
            AtomicInteger joined = new AtomicInteger();
            ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
            List<Thread> threads = new ArrayList<>();

            for (int i = 0; i < 20; i++) {
                Thread t = new Thread(() -> {
                    try {
                        go.await();
                        room.addPlayer(fakePlayer());
                        joined.incrementAndGet();
                    } catch (IllegalStateException | RoomFullException e) {
                        // vào muộn bị từ chối: bình thường
                    } catch (Throwable e) {
                        errors.add(e);
                    }
                });
                threads.add(t);
                t.start();
            }
            Thread starter = new Thread(() -> {
                try {
                    go.await();
                    // Người mới vào chen giữa chưa READY thì startGame từ chối: thử lại cho tới khi được
                    for (int tries = 0; tries < 1000 && room.getState() != RoomState.PLAYING; tries++) {
                        ready(room);
                        try {
                            room.startGame(map(), 30);
                        } catch (IllegalStateException notReadyYet) {
                            Thread.yield();
                        }
                    }
                } catch (Throwable e) {
                    errors.add(e);
                }
            });
            threads.add(starter);
            starter.start();

            go.countDown();
            joinAllOrFail(threads);
            try {
                assertEquals(0, errors.size());
                assertEquals(RoomState.PLAYING, room.getState());
                // 2 người ban đầu + số người vào thành công phải đúng bằng số người trong phòng
                assertEquals(2 + joined.get(), room.getPlayerCount());
                // và bảng điểm phải có đúng từng ấy người: không ai "lọt lưới" sau khi chụp danh sách
                assertEquals(room.getPlayerCount(), room.getScores().size());
            } finally {
                room.stopGame();
            }
        }
    }

    // ---------------------------------------------------------------------------------
    // 3. Nhiều phòng đang tick, người chơi rời liên tục + bấm xin kết thúc + gửi lệnh di chuyển:
    //    mọi phòng phải kết thúc gọn, tick dừng hẳn, không đứng máy.
    // ---------------------------------------------------------------------------------
    @Test
    void manyRoomsTicking_playersLeaveAtRandom_allEndCleanly() throws Exception {
        final int rooms = 10;
        final int playersPerRoom = 5;
        List<GameRoom> roomList = new ArrayList<>();
        List<List<ClientSession>> playersOfRoom = new ArrayList<>();

        for (int r = 0; r < rooms; r++) {
            GameRoom room = new GameRoom("room-" + r, playersPerRoom);
            List<ClientSession> players = new ArrayList<>();
            for (int i = 0; i < playersPerRoom; i++) {
                ClientSession p = fakePlayer();
                room.addPlayer(p);
                players.add(p);
            }
            ready(room);
            room.startGame(map(), 60);
            roomList.add(room);
            playersOfRoom.add(players);
        }

        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
        CountDownLatch go = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();

        for (int r = 0; r < rooms; r++) {
            final GameRoom room = roomList.get(r);
            final List<ClientSession> players = playersOfRoom.get(r);

            // Thread "đổ lệnh di chuyển" liên tục
            Thread mover = new Thread(() -> {
                try {
                    go.await();
                    for (int k = 0; k < 3000; k++) {
                        room.submitMove(players.get(k % players.size()).getPlayerId(),
                                k % 3 == 0 ? null : tankops.common.model.common.Direction.UP);
                    }
                } catch (Throwable e) {
                    errors.add(e);
                }
            });
            // Thread "người chơi lần lượt thoát"
            Thread leaver = new Thread(() -> {
                try {
                    go.await();
                    for (ClientSession p : players) {
                        Thread.sleep(5);
                        room.removePlayer(p);
                    }
                } catch (Throwable e) {
                    errors.add(e);
                }
            });
            threads.add(mover);
            threads.add(leaver);
            mover.start();
            leaver.start();
        }
        go.countDown();
        joinAllOrFail(threads);

        assertEquals(0, errors.size());
        for (GameRoom room : roomList) {
            waitUntil(() -> room.getState() != RoomState.PLAYING);
            // Hết ván rồi mà còn người rời phòng thì phòng quay về LOBBY (chơi lại được): cả hai đều đúng
            RoomState end = room.getState();
            assertTrue(end == RoomState.FINISHED || end == RoomState.LOBBY, "trạng thái cuối phải là FINISHED hoặc LOBBY, nhưng là " + end);
        }

        // Tick của mọi phòng phải đã dừng hẳn
        Thread.sleep(200);
        List<Integer> before = new ArrayList<>();
        for (GameRoom room : roomList) {
            before.add(room.getTickCount());
        }
        Thread.sleep(300);
        for (int r = 0; r < rooms; r++) {
            assertEquals((int) before.get(r), roomList.get(r).getTickCount());
        }
    }

    // ---------------------------------------------------------------------------------
    // 4. Bật/tắt vòng tick rất nhiều lần: không được rò thread (thread chạy mãi không dừng)
    // ---------------------------------------------------------------------------------
    @Test
    void startStopManyTimes_noThreadLeak() throws Exception {
        int before = Thread.activeCount();

        for (int i = 0; i < 200; i++) {
            GameRoom room = new GameRoom("room-" + i, 5);
            room.addPlayer(fakePlayer());
            room.addPlayer(fakePlayer());
            ready(room);
            room.startGame(map(), 30);
            room.stopGame();
        }

        Thread.sleep(500); // cho các thread kịp thoát
        int after = Thread.activeCount();
        assertTrue(after <= before + 10); // 200 vòng mà không rò thì chênh rất ít
    }

    // ---------------------------------------------------------------------------------
    // 5. Tầng mạng (T04): kết nối thật. Server đầy thì từ chối; ngắt hết thì dọn sạch.
    // ---------------------------------------------------------------------------------
    @Test
    void connections_serverFullRejectsExtra_andCleansUpAfterDisconnect() throws Exception {
        GameServer server = new GameServer();
        int max = server.getMaxClients();
        // cổng 0 = hệ điều hành chọn cổng trống, khỏi đụng 5555. Số 500 = hàng đợi chờ đủ lớn (mặc định chỉ 50)
        ServerSocket serverSocket = new ServerSocket(0, 500);
        new Thread(new ConnectionAcceptor(serverSocket, server)).start();

        List<Socket> clients = new ArrayList<>();
        try {
            for (int i = 0; i < max + 50; i++) {
                clients.add(new Socket("localhost", serverSocket.getLocalPort()));
            }
            waitUntil(() -> server.getSessionCount() >= max);
            Thread.sleep(300); // chờ thêm xem có lọt quá số tối đa không

            assertEquals(max, server.getSessionCount()); // đúng bằng max, không hơn

            for (Socket s : clients) {
                s.close();
            }
            waitUntil(() -> server.getSessionCount() == 0);
            assertEquals(0, server.getSessionCount()); // ngắt hết thì danh sách phải rỗng, không sót
        } finally {
            for (Socket s : clients) {
                s.close();
            }
            serverSocket.close(); // đóng cổng thì thread acceptor tự thoát
        }
    }

    // Bão kết nối: nhiều thread liên tục kết nối rồi ngắt ngay => cuối cùng không sót session nào
    @Test
    void connectionStorm_noLeakedSessions() throws Exception {
        GameServer server = new GameServer();
        ServerSocket serverSocket = new ServerSocket(0, 500); // hàng đợi chờ lớn để HĐH không từ chối
        new Thread(new ConnectionAcceptor(serverSocket, server)).start();

        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
        AtomicInteger connected = new AtomicInteger();

        try {
            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Thread t = new Thread(() -> {
                    try {
                        for (int k = 0; k < 30; k++) {
                            Socket s = connectWithRetry(serverSocket.getLocalPort());
                            if (s != null) {
                                connected.incrementAndGet();
                                s.close();
                            }
                        }
                    } catch (Throwable e) {
                        errors.add(e);
                    }
                });
                threads.add(t);
                t.start();
            }
            joinAllOrFail(threads);

            // Chờ cho đến khi số session về 0 VÀ giữ nguyên 0 một lúc (server còn đang xử lý hàng đợi chờ)
            int zeroInARow = 0;
            for (int i = 0; i < 500 && zeroInARow < 30; i++) {
                zeroInARow = (server.getSessionCount() == 0) ? zeroInARow + 1 : 0;
                Thread.sleep(10);
            }

            assertEquals(0, errors.size()); // nếu có lỗi lạ, JUnit sẽ cho biết lỗi gì ở dòng dưới
            assertTrue(connected.get() > 0);
            assertEquals(0, server.getSessionCount());
        } finally {
            serverSocket.close();
        }
    }

    // Kết nối, nếu bị hệ điều hành từ chối tạm thời (hàng đợi chờ đầy) thì đợi một chút rồi thử lại
    private Socket connectWithRetry(int port) throws InterruptedException {
        for (int attempt = 0; attempt < 20; attempt++) {
            try {
                return new Socket("localhost", port);
            } catch (java.io.IOException e) {
                Thread.sleep(10);
            }
        }
        return null; // thử 20 lần vẫn không được thì bỏ qua lần kết nối này
    }
}