package tankops.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import tankops.common.map.GameMap;
import tankops.common.map.MapLoader;
import tankops.common.model.tank.Tank;
import tankops.server.net.ClientSession;

/** T21: vòng đời phòng LOBBY -> PLAYING -> FINISHED và điểm số. */
class RoomLifecycleTest {

    private GameMap map() {
        return new MapLoader().loadMap("/maps/map_multi.map");
    }

    private ClientSession fakePlayer() {
        return new ClientSession(null, null);
    }

    private void waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 200 && !condition.getAsBoolean(); i++) {
            Thread.sleep(10);
        }
    }

    // Phòng mới tạo thì ở LOBBY
    @Test
    void newRoom_isLobby() {
        assertEquals(RoomState.LOBBY, new GameRoom("room-1", 5).getState());
    }

    // Chỉ 1 người thì không được bắt đầu, và vẫn ở LOBBY
    @Test
    void startGame_onePlayer_refused() throws Exception {
        GameRoom room = new GameRoom("room-1", 5);
        room.addPlayer(fakePlayer());
        assertThrows(IllegalStateException.class, () -> room.startGame(map(), 30));
        assertEquals(RoomState.LOBBY, room.getState());
    }

    // 2 người: LOBBY -> PLAYING. Bắt đầu lần thứ hai thì bị từ chối
    @Test
    void startGame_twoPlayers_becomesPlaying_andCannotStartTwice() throws Exception {
        GameRoom room = new GameRoom("room-1", 5);
        room.addPlayer(fakePlayer());
        room.addPlayer(fakePlayer());
        room.startGame(map(), 30);
        try {
            assertEquals(RoomState.PLAYING, room.getState());
            assertThrows(IllegalStateException.class, () -> room.startGame(map(), 30));
        } finally {
            room.stopGame();
        }
    }

    // Phòng đang chơi thì người mới không vào được
    @Test
    void addPlayer_whilePlaying_refused() throws Exception {
        GameRoom room = new GameRoom("room-1", 5);
        room.addPlayer(fakePlayer());
        room.addPlayer(fakePlayer());
        room.startGame(map(), 30);
        try {
            assertThrows(IllegalStateException.class, () -> room.addPlayer(fakePlayer()));
            assertEquals(2, room.getPlayerCount());
        } finally {
            room.stopGame();
        }
    }

    // Xin kết thúc: PLAYING -> FINISHED, tick dừng, 2 xe còn sống nên hòa (không có người thắng)
    @Test
    void requestFinish_becomesFinished_tickStops() throws Exception {
        GameRoom room = new GameRoom("room-1", 5);
        room.addPlayer(fakePlayer());
        room.addPlayer(fakePlayer());
        room.startGame(map(), 30);

        room.requestFinish();
        waitUntil(() -> room.getState() == RoomState.FINISHED);

        assertEquals(RoomState.FINISHED, room.getState());
        assertNull(room.getWinnerId());

        Thread.sleep(100);
        int after = room.getTickCount();
        Thread.sleep(200);
        assertEquals(after, room.getTickCount()); // tick đã dừng hẳn
    }

    // Chỉ còn 1 xe sống => xe đó thắng, ván tự kết thúc
    @Test
    void lastTankAlive_wins() throws Exception {
        GameRoom room = new GameRoom("room-1", 5);
        ClientSession a = fakePlayer();
        ClientSession b = fakePlayer();
        room.addPlayer(a);
        room.addPlayer(b);
        room.startGame(map(), 30);

        // Giả lập xe của b bị hạ. Phải dùng takeDamage (như đạn thật), vì xe người chơi giờ có "mạng":
        // chỉ đặt máu = 0 thì xe không bị coi là chết. Sau này đạn của M3 sẽ gây sát thương theo đường này.
        Tank tankOfB = room.getEngineForTest().getGameState().getTankById(b.getPlayerId());
        tankOfB.takeDamage(tankOfB.getMaxHealth());

        waitUntil(() -> room.getState() == RoomState.FINISHED);
        assertEquals(RoomState.FINISHED, room.getState());
        assertEquals(a.getPlayerId(), room.getWinnerId());
    }

    // Đang chơi mà 1 người thoát, chỉ còn 1 người => ván kết thúc
    @Test
    void playerLeaves_leavingOnePlayer_finishes() throws Exception {
        GameRoom room = new GameRoom("room-1", 5);
        ClientSession a = fakePlayer();
        ClientSession b = fakePlayer();
        room.addPlayer(a);
        room.addPlayer(b);
        room.startGame(map(), 30);

        room.removePlayer(b);
        waitUntil(() -> room.getState() == RoomState.FINISHED);
        assertEquals(RoomState.FINISHED, room.getState());
    }

    // Mọi người thoát hết => tick tắt ngay
    @Test
    void everyoneLeaves_tickStops() throws Exception {
        GameRoom room = new GameRoom("room-1", 5);
        ClientSession a = fakePlayer();
        ClientSession b = fakePlayer();
        room.addPlayer(a);
        room.addPlayer(b);
        room.startGame(map(), 30);

        room.removePlayer(a);
        room.removePlayer(b);
        assertEquals(RoomState.FINISHED, room.getState());

        Thread.sleep(100);
        int after = room.getTickCount();
        Thread.sleep(200);
        assertEquals(after, room.getTickCount());
    }

    // Điểm: bắt đầu 0, cộng điểm khi hạ xe địch, getScores trả bản sao
    @Test
    void scores_startAtZero_addKillAdds_copyIsSafe() throws Exception {
        GameRoom room = new GameRoom("room-1", 5);
        ClientSession a = fakePlayer();
        ClientSession b = fakePlayer();
        room.addPlayer(a);
        room.addPlayer(b);
        room.startGame(map(), 30);
        try {
            assertEquals(0, room.getScores().get(a.getPlayerId()));

            room.addKill(a.getPlayerId());
            room.addKill(a.getPlayerId());
            assertEquals(2 * GameRoom.KILL_POINTS, room.getScores().get(a.getPlayerId()));
            assertEquals(0, room.getScores().get(b.getPlayerId()));

            room.getScores().put(a.getPlayerId(), 999999); // sửa vào bản sao thì không ảnh hưởng
            assertTrue(room.getScores().get(a.getPlayerId()) < 999999);
        } finally {
            room.stopGame();
        }
    }

    // Nhiều thread cùng cộng điểm: tổng phải đúng (không mất điểm)
    @Test
    void addScore_manyThreads_noLostUpdate() throws Exception {
        GameRoom room = new GameRoom("room-1", 5);
        ClientSession a = fakePlayer();
        room.addPlayer(a);
        room.addPlayer(fakePlayer());
        room.startGame(map(), 30);
        try {
            Thread[] threads = new Thread[20];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 500; j++) {
                        room.addScore(a.getPlayerId(), 1);
                    }
                });
                threads[i].start();
            }
            for (Thread t : threads) {
                t.join();
            }
            assertEquals(20 * 500, room.getScores().get(a.getPlayerId()));
        } finally {
            room.stopGame();
        }
    }
}