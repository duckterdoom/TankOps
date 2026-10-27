package tankops.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import tankops.common.map.MapLoader;
import tankops.common.model.common.Vector2D;
import tankops.common.model.environment.BrickWall;
import tankops.common.model.environment.Environment;
import tankops.common.model.powerups.Star;
import tankops.common.net.game.EnvironmentSnapshot;
import tankops.common.net.game.PowerUpSnapshot;
import tankops.common.net.game.SnapshotMessage;
import tankops.server.net.ClientSession;

// Snapshot phải mang cả vật phẩm và tường (gạch/thép) chứ không chỉ xe và đạn
class SnapshotContentTest {

    private GameRoom startedRoom() throws Exception {
        GameRoom room = new GameRoom("room-1", 5);
        room.addPlayer(new ClientSession(null, null));
        room.addPlayer(new ClientSession(null, null));
        room.setPlayerReady(room.getPlayers().get(1).getPlayerName(), true);
        // tick chậm (1 lần/giây) để test sửa engine không bị tick thread chen ngang quá nhiều
        room.startGame(new MapLoader().loadMap("/maps/map_multi.map"), 1);
        return room;
    }

    // Chỉ gạch/thép được gửi; số lượng khớp với bản đồ; xe, đạn vẫn có
    @Test
    void snapshot_hasBrickAndSteelWalls() throws Exception {
        GameRoom room = startedRoom();
        try {
            SnapshotMessage snap = room.buildBinarySnapshot();
            assertTrue(snap.environments().size() > 0, "bản đồ có tường nên snapshot phải có tường");
            for (EnvironmentSnapshot e : snap.environments()) {
                assertTrue(e.kind().equals("BrickWall") || e.kind().equals("SteelWall"),
                        "chỉ gạch/thép được gửi, nhưng gặp " + e.kind());
            }
            assertEquals(2, snap.tanks().size());
        } finally {
            room.stopGame();
        }
    }

    // Vật phẩm đặt trong engine phải xuất hiện trong snapshot với đúng loại và vị trí
    @Test
    void snapshot_hasPowerUps() throws Exception {
        GameRoom room = startedRoom();
        try {
            // chờ tick đầu tiên chạy xong rồi mới đụng engine (tick thread chạy mỗi 1 giây)
            for (int i = 0; i < 300 && room.getTickCount() < 1; i++) {
                Thread.sleep(10);
            }
            // tick kế tiếp còn ~1 giây nữa nên có thời gian đặt vật phẩm
            room.getEngineForTest().getGameState().addPowerUp(new Star("pu-test", new Vector2D(64.0, 96.0)));
            SnapshotMessage snap = room.buildBinarySnapshot();
            PowerUpSnapshot found = null;
            for (PowerUpSnapshot p : snap.powerUps()) {
                if (p.id().equals("pu-test")) {
                    found = p;
                }
            }
            assertTrue(found != null, "vật phẩm vừa đặt phải có trong snapshot");
            assertEquals("Star", found.kind());
            assertEquals(64.0, found.x());
            assertEquals(96.0, found.y());
        } finally {
            room.stopGame();
        }
    }

    // Tường bị phá thì biến mất khỏi snapshot
    @Test
    void destroyedWall_disappearsFromSnapshot() throws Exception {
        GameRoom room = startedRoom();
        try {
            for (int i = 0; i < 300 && room.getTickCount() < 1; i++) {
                Thread.sleep(10);
            }
            int before = room.buildBinarySnapshot().environments().size();
            BrickWall target = null;
            for (Environment env : room.getEngineForTest().getGameState().getEnvironments()) {
                if (env instanceof BrickWall b && !b.isDestroyed()) {
                    target = b;
                    break;
                }
            }
            assertTrue(target != null, "bản đồ phải có ít nhất một tường gạch");
            for (int i = 0; i < 10; i++) {
                target.takeHit(1);
            }
            int after = room.buildBinarySnapshot().environments().size();
            assertEquals(before - 1, after);
        } finally {
            room.stopGame();
        }
    }
}
