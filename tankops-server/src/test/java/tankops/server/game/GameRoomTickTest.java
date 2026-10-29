package tankops.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import tankops.common.map.MapLoader;
import tankops.common.model.common.Direction;
import tankops.server.net.ClientSession;

class GameRoomTickTest {

    // Từ khi có READY: mọi người trừ chủ phòng (người đầu tiên) phải READY thì mới START được
    private static void ready(GameRoom room) {
        java.util.List<tankops.server.net.ClientSession> ps = room.getPlayers();
        for (int i = 1; i < ps.size(); i++) {
            room.setPlayerReady(ps.get(i).getPlayerName(), true);
        }
    }


    // Chờ tối đa 2 giây cho đến khi điều kiện đúng (tránh test chạy chập chờn)
    private void waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 200 && !condition.getAsBoolean(); i++) {
            Thread.sleep(10);
        }
    }

    // Phòng có 2 người (T21: cần tối thiểu 2 người mới được bắt đầu)
    private GameRoom roomWithTwoPlayers() throws Exception {
        GameRoom room = new GameRoom("room-1", 5);
        room.addPlayer(new ClientSession(null, null));
        room.addPlayer(new ClientSession(null, null));
        return room;
    }

    // Vòng tick phải thật sự chạy: tickCount tăng lên
    @Test
    void startGame_tickCountIncreases() throws Exception {
        GameRoom room = roomWithTwoPlayers();
        ready(room);
        room.startGame(new MapLoader().loadMap("/maps/map_multi.map"), 30);
        try {
            waitUntil(() -> room.getTickCount() >= 10);
            assertTrue(room.getTickCount() >= 10);
        } finally {
            room.stopGame();
        }
    }

    // Sau khi stopGame thì tick phải dừng hẳn
    @Test
    void stopGame_tickStops() throws Exception {
        GameRoom room = roomWithTwoPlayers();
        ready(room);
        room.startGame(new MapLoader().loadMap("/maps/map_multi.map"), 30);
        waitUntil(() -> room.getTickCount() >= 3);
        room.stopGame();
        Thread.sleep(100); // chờ tick đang dở (nếu có) chạy xong
        int after = room.getTickCount();
        Thread.sleep(200);
        assertEquals(after, room.getTickCount());
    }

    // Nhiều thread cùng bỏ lệnh vào hàng đợi trong lúc tick đang chạy: không được lỗi/đứng
    @Test
    void submitMove_manyThreads_noCrash() throws Exception {
        GameRoom room = new GameRoom("room-1", 5);
        ClientSession p = new ClientSession(null, null);
        room.addPlayer(p);
        room.addPlayer(new ClientSession(null, null));
        ready(room);
        room.startGame(new MapLoader().loadMap("/maps/map_multi.map"), 30);
        try {
            Thread[] threads = new Thread[10];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 200; j++) {
                        room.submitMove(p.getPlayerId(), j % 2 == 0 ? Direction.DOWN : null);
                    }
                });
                threads[i].start();
            }
            for (Thread t : threads) {
                t.join();
            }
            int before = room.getTickCount();
            waitUntil(() -> room.getTickCount() > before + 5);
            assertTrue(room.getTickCount() > before + 5); // tick vẫn sống sau cơn bão lệnh
        } finally {
            room.stopGame();
        }
    }
}