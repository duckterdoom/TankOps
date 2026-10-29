package tankops.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import tankops.common.map.GameMap;
import tankops.common.map.MapLoader;
import tankops.common.model.tank.Tank;
import tankops.server.net.ClientSession;

// Sau khi hết ván: có số kill/death, và người chơi vào lại phòng thì thấy phòng chờ (LOBBY) để chơi tiếp
class PlayAgainTest {

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

    // Chơi xong một ván: a hạ b. Trả về phòng ở trạng thái FINISHED
    private GameRoom finishedRoom(ClientSession a, ClientSession b) throws Exception {
        GameRoom room = new GameRoom("room-1", 5);
        room.addPlayer(a);
        room.addPlayer(b);
        room.setPlayerReady(b.getPlayerName(), true);
        room.startGame(map(), 30);

        room.addKill(a.getPlayerName()); // a hạ b: server cộng kill + điểm
        Tank tankOfB = room.getEngineForTest().getGameState().getTankById(b.getPlayerName());
        tankOfB.takeDamage(tankOfB.getMaxHealth());

        waitUntil(() -> room.getState() == RoomState.FINISHED);
        assertEquals(RoomState.FINISHED, room.getState());
        return room;
    }

    // Số kill và death đúng: a có 1 kill, b chết 1 lần
    @Test
    void killsAndDeaths_areCounted() throws Exception {
        ClientSession a = fakePlayer();
        ClientSession b = fakePlayer();
        GameRoom room = finishedRoom(a, b);

        assertEquals(1, (int) room.getKills().get(a.getPlayerName()));
        assertEquals(0, (int) room.getKills().get(b.getPlayerName()));
        assertEquals(0, (int) room.getDeaths().get(a.getPlayerName()));
        assertEquals(1, (int) room.getDeaths().get(b.getPlayerName()));
        assertEquals(GameRoom.KILL_POINTS, (int) room.getScores().get(a.getPlayerName()));
    }

    // Người đang ở phòng vừa chơi xong bấm vào lại đúng phòng đó: thành công, phòng về LOBBY, READY đã xóa
    @Test
    void rejoinSameRoom_afterFinished_backToLobbyWithReadyCleared() throws Exception {
        RoomManager manager = new RoomManager(10, 5);
        GameRoom room = manager.createRoom();
        ClientSession a = fakePlayer();
        ClientSession b = fakePlayer();
        manager.joinRoom(room.getId(), a);
        manager.joinRoom(room.getId(), b);
        room.setPlayerReady(b.getPlayerName(), true);
        room.startGame(map(), 30);
        Tank tankOfB = room.getEngineForTest().getGameState().getTankById(b.getPlayerName());
        tankOfB.takeDamage(tankOfB.getMaxHealth());
        waitUntil(() -> room.getState() == RoomState.FINISHED);
        assertEquals(RoomState.FINISHED, room.getState());

        GameRoom again = manager.joinOrCreate(room.getId(), b); // b bấm lại phòng cũ
        assertTrue(again == room);
        assertEquals(RoomState.LOBBY, room.getState());
        assertTrue(!room.isPlayerReady(b.getPlayerName()));
        assertEquals(2, room.getPlayerCount()); // không bị thêm lần hai
    }

    // Rời phòng sau khi hết ván: phòng về LOBBY, người còn lại vẫn ở trong
    @Test
    void leaveAfterFinished_roomBecomesLobby() throws Exception {
        ClientSession a = fakePlayer();
        ClientSession b = fakePlayer();
        GameRoom room = finishedRoom(a, b);

        room.removePlayer(b);
        assertEquals(RoomState.LOBBY, room.getState());
        assertEquals(1, room.getPlayerCount());
    }

    // Chơi ván thứ hai trong cùng phòng: điểm, kill, death bắt đầu lại từ 0
    @Test
    void secondGame_startsFromZero() throws Exception {
        ClientSession a = fakePlayer();
        ClientSession b = fakePlayer();
        GameRoom room = finishedRoom(a, b);

        room.resetToLobbyIfFinished();
        room.setPlayerReady(b.getPlayerName(), true);
        room.startGame(map(), 30);
        try {
            assertEquals(RoomState.PLAYING, room.getState());
            assertEquals(0, (int) room.getScores().get(a.getPlayerName()));
            assertEquals(0, (int) room.getKills().get(a.getPlayerName()));
            assertEquals(0, (int) room.getDeaths().get(b.getPlayerName()));
        } finally {
            room.stopGame();
        }
    }
}
