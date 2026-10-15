package tankops.common.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tankops.common.map.GameMap;
import tankops.common.model.common.Direction;
import tankops.common.model.common.Vector2D;
import tankops.common.model.environment.Environment;
import tankops.common.model.tank.Tank;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameEngineTest {

    private GameEngine gameEngine;
    private final String playerId = "player_1";
    private final Vector2D startPos = new Vector2D(100.0, 100.0);

    // Mock lớp Environment có thuộc tính cản đường để test va chạm
    private static class MockObstacleWall extends Environment {
        public MockObstacleWall(String id, Vector2D position) {
            super(id, position);
        }

        @Override public boolean canTankPass() { return false; }
        @Override public boolean canBulletPass() { return false; }
        @Override public void takeHit(int starLevel) {}
        @Override public boolean isDestroyed() { return false; }
    }

    // Mock lớp Environment cho phép đi qua (ví dụ: cỏ, nước...)
    private static class MockPassableTile extends Environment {
        public MockPassableTile(String id, Vector2D position) {
            super(id, position);
        }

        @Override public boolean canTankPass() { return true; }
        @Override public boolean canBulletPass() { return true; }
        @Override public void takeHit(int starLevel) {}
        @Override public boolean isDestroyed() { return false; }
    }

    @BeforeEach
    void setUp() {
        GameMap emptyMap = new GameMap(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        gameEngine = new GameEngine(emptyMap);
        gameEngine.spawnPlayers(List.of(playerId), List.of(startPos));
    }

    @Test
    @DisplayName("Xe tăng di chuyển bình thường khi không gặp vật cản")
    void testTankMovesWithoutCollision() {
        // Client bấm phím sang PHẢI (RIGHT)
        gameEngine.handleMoveEvent(playerId, Direction.RIGHT);
        gameEngine.update();

        Tank tank = gameEngine.getGameState().getTankById(playerId);
        assertNotNull(tank, "Tank phải tồn tại trong GameState");
        assertEquals(Direction.RIGHT, tank.getDirection(), "Hướng quay mặt phải đổi sang RIGHT");
        assertTrue(tank.getPosition().x() > startPos.x(), "Tọa độ X phải tăng sau khi di chuyển sang phải");
        assertEquals(startPos.y(), tank.getPosition().y(), "Tọa độ Y phải giữ nguyên");
    }

    @Test
    @DisplayName("TC02: Xe tăng dừng lại và không thay đổi vị trí khi đụng tường (canTankPass = false)")
    void testTC02_TankCollidesWithWallAndRollsBack() {
        // Đặt tường cản đường ngay vị trí tiếp theo (ví dụ: (102, 100))
        MockObstacleWall wall = new MockObstacleWall("wall_1", new Vector2D(102.0, 100.0));
        gameEngine.getGameState().getEnvironments().add(wall);

        // Gửi lệnh di chuyển sang PHẢI (RIGHT) đâm thẳng vào tường
        gameEngine.handleMoveEvent(playerId, Direction.RIGHT);
        gameEngine.update();

        Tank tank = gameEngine.getGameState().getTankById(playerId);
        assertNotNull(tank);

        // Vị trí không thay đổi (bị rollback về vị trí cũ ban đầu)
        assertEquals(startPos.x(), tank.getPosition().x(), "Tọa độ X không được thay đổi do vướng tường");
        assertEquals(startPos.y(), tank.getPosition().y(), "Tọa độ Y không được thay đổi");
    }



    @Test
    @DisplayName("Xe tăng đi xuyên qua vùng môi trường cho phép (canTankPass = true)")
    void testTankPassesThroughPassableEnvironment() {
        // Đặt ngói/cỏ cho phép đi qua ngay phía trước
        MockPassableTile tile = new MockPassableTile("grass_1", new Vector2D(102.0, 100.0));
        gameEngine.getGameState().getEnvironments().add(tile);

        gameEngine.handleMoveEvent(playerId, Direction.RIGHT);
        gameEngine.update();

        Tank tank = gameEngine.getGameState().getTankById(playerId);
        assertNotNull(tank);
        assertTrue(tank.getPosition().x() > startPos.x(), "Xe tăng phải đi qua được vùng passable");
    }

    @Test
    @DisplayName("Xe tăng dừng lại khi người chơi nhả phím (direction = null)")
    void testTankStopsMovingOnKeyRelease() {
        // 1. Nhấn phím LÊN (UP)
        gameEngine.handleMoveEvent(playerId, Direction.UP);
        gameEngine.update();

        Tank tank = gameEngine.getGameState().getTankById(playerId);
        Vector2D posAfterFirstTick = tank.getPosition();

        // 2. Nhả phím (gửi null)
        gameEngine.handleMoveEvent(playerId, null);
        gameEngine.update();

        // Vị trí sau nhịp tick thứ 2 giữ nguyên
        assertEquals(posAfterFirstTick.x(), tank.getPosition().x());
        assertEquals(posAfterFirstTick.y(), tank.getPosition().y());
    }
}