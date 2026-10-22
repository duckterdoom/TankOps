package tankops.client.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tankops.common.model.common.Direction;
import tankops.common.net.game.BulletSnapshot;
import tankops.common.net.game.GameOverMessage;
import tankops.common.net.lobby.JoinResponseMessage;
import tankops.common.net.game.SnapshotMessage;
import tankops.common.net.game.TankSnapshot;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử GameStateStore - Quản lý trạng thái Client")
class GameStateStoreTest {

    @Test
    @DisplayName("Khởi tạo ban đầu ở trạng thái rỗng")
    void initialState_isEmpty() {
        GameStateStore store = new GameStateStore();
        assertNull(store.getLatestSnapshot());
        assertTrue(store.getTanks().isEmpty());
        assertTrue(store.getBullets().isEmpty());
        assertEquals(-1, store.getCurrentTick());
        assertFalse(store.isInGame());
        assertFalse(store.isGameOver());
    }

    @Test
    @DisplayName("Nhận JoinResponseMessage thành công -> chuyển sang isInGame = true")
    void onJoinResponseSuccess_setsInGame() {
        GameStateStore store = new GameStateStore();
        AtomicBoolean changed = new AtomicBoolean(false);
        store.addStateChangeListener(() -> changed.set(true));

        store.onMessageReceived(new JoinResponseMessage(true, "OK", "room-1", "player-1", 1));

        assertTrue(store.isInGame());
        assertEquals("room-1", store.getCurrentRoomId());
        assertEquals("player-1", store.getAssignedPlayerId());
        assertTrue(changed.get());
    }

    @Test
    @DisplayName("Nhận SnapshotMessage -> cập nhật dữ liệu và danh sách xe, đạn")
    void onSnapshotMessage_updatesState() {
        GameStateStore store = new GameStateStore();

        TankSnapshot tank = new TankSnapshot("tank-1", 10.0, 20.0, Direction.RIGHT, 100, 100, 1, false, true, false);
        BulletSnapshot bullet = new BulletSnapshot("b-1", "tank-1", 15.0, 25.0, Direction.RIGHT);
        SnapshotMessage snap = new SnapshotMessage(42, List.of(tank), List.of(bullet), false, null);

        store.onMessageReceived(snap);

        assertNotNull(store.getLatestSnapshot());
        assertEquals(42, store.getCurrentTick());
        assertEquals(1, store.getTanks().size());
        assertEquals("tank-1", store.getTanks().get(0).id());
        assertEquals(1, store.getBullets().size());
        assertEquals("b-1", store.getBullets().get(0).id());
    }

    @Test
    @DisplayName("Nhận GameOverMessage -> kết thúc ván và lưu điểm")
    void onGameOverMessage_setsGameOver() {
        GameStateStore store = new GameStateStore();
        store.onMessageReceived(new JoinResponseMessage(true, "OK", "room-1", "player-1", 1));
        assertTrue(store.isInGame());

        store.onMessageReceived(new GameOverMessage("player-1", Map.of("player-1", 200, "player-2", 0)));

        assertFalse(store.isInGame());
        assertTrue(store.isGameOver());
        assertEquals("player-1", store.getGameOverState().winnerId());
        assertEquals(200, store.getScores().get("player-1"));
    }
}
