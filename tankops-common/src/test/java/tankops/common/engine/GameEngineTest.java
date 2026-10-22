package tankops.common.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tankops.common.map.GameMap;
import tankops.common.model.common.Bullet;
import tankops.common.model.common.Direction;
import tankops.common.model.common.Vector2D;
import tankops.common.model.environment.Environment;
import tankops.common.model.tank.PlayerTank;
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
        private boolean hitCalled = false;
        private int lastStarLevel = -1;

        public MockObstacleWall(String id, Vector2D position) {
            super(id, position);
        }

        @Override public boolean canTankPass() { return false; }
        @Override public boolean canBulletPass() { return false; }
        @Override public void takeHit(int starLevel) {
            this.hitCalled = true;
            this.lastStarLevel = starLevel;
        }
        @Override public boolean isDestroyed() { return false; }

        public boolean isHitCalled() { return hitCalled; }
        public int getLastStarLevel() { return lastStarLevel; }
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
        GameMap emptyMap = new GameMap(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 25, 19);

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

    @Test
    @DisplayName("Boundary Check: Xe tăng đứng sát mép biên (X = 0, Y = 0) vẫn được coi là hợp lệ (Boundary Valid)")
    void testTankValidAtExactCornerBoundary() {
        Tank tank = gameEngine.getGameState().getTankById(playerId);

        // Đặt xe tăng vừa đúng mép trên-trái (0.0, 0.0)
        tank.setPosition(new Vector2D(0.0, 0.0));

        // Gọi update mà không di chuyển
        gameEngine.update();

        // Xe tăng vẫn tồn tại và ở nguyên vị trí (0.0, 0.0) mà không bị lỗi
        assertEquals(0.0, tank.getPosition().x(), "Tọa độ X tại mép 0.0 phải hợp lệ");
        assertEquals(0.0, tank.getPosition().y(), "Tọa độ Y tại mép 0.0 phải hợp lệ");
    }

    @Test
    @DisplayName("Boundary Check: Xe tăng đứng sát mép phải (X = mapWidth - TANK_SIZE) vẫn hợp lệ")
    void testTankValidAtExactRightBoundary() {
        Tank tank = gameEngine.getGameState().getTankById(playerId);
        double exactRightX = gameEngine.getMapWidth() - Tank.TANK_SIZE;
        double exactBottomY = gameEngine.getMapHeight() - Tank.TANK_SIZE;

        // Đặt xe tăng vừa khít mép dưới-phải
        tank.setPosition(new Vector2D(exactRightX, exactBottomY));
        gameEngine.update();

        assertEquals(exactRightX, tank.getPosition().x(), "Vị trí sát mép phải phải hợp lệ");
        assertEquals(exactBottomY, tank.getPosition().y(), "Vị trí sát mép dưới phải hợp lệ");
    }

    // =========================================================================
    // TEST CASES FOR BULLET LOGIC
    // =========================================================================

    @Test
    @DisplayName("Sự kiện bắn tạo ra Bullet thành công trong GameState")
    void testHandleShootEventCreatesBullet() {
        gameEngine.handleShootEvent(playerId);

        List<Bullet> bullets = gameEngine.getGameState().getBullets();
        assertEquals(1, bullets.size(), "Phải có đúng 1 viên đạn được thêm vào GameState");

        Bullet bullet = bullets.get(0);
        assertEquals(playerId, bullet.getOwnerId(), "Chủ sở hữu đạn phải khớp với playerId");
    }

    @Test
    @DisplayName("Đạn di chuyển bình thường theo từng nhịp tick update()")
    void testBulletMovesOnUpdate() {
        gameEngine.handleShootEvent(playerId);
        Bullet bullet = gameEngine.getGameState().getBullets().get(0);
        Vector2D initialBulletPos = bullet.getPosition();

        gameEngine.update();

        assertNotEquals(initialBulletPos, bullet.getPosition(), "Vị trí của đạn phải thay đổi sau khi update");
    }

    @Test
    @DisplayName("Đạn tự động bị xóa khỏi GameState khi bay ra khỏi biên bản đồ")
    void testBulletRemovedWhenOutOfBounds() {
        // Đặt đạn ở sát biên phải bản đồ và hướng sang phải
        double nearRightX = gameEngine.getMapWidth() - Bullet.SIZE + 1.0;
        Bullet bullet = new Bullet("bullet_out", playerId, new Vector2D(nearRightX, 100.0), Direction.RIGHT, Bullet.SPEED_FAST, 1, 0);

        gameEngine.getGameState().addBullet(bullet);
        assertEquals(1, gameEngine.getGameState().getBullets().size());

        // Update nhịp tick cho đạn bay ra ngoài
        gameEngine.update();

        assertTrue(gameEngine.getGameState().getBullets().isEmpty(), "Đạn bay ra ngoài biên map phải bị xóa khỏi GameState");
    }

    @Test
    @DisplayName("TC03: Đạn va chạm với tường (canBulletPass = false) sẽ gây sát thương tường và bị xóa")
    void testTC03_BulletCollidesWithObstacleWallAndRemoves() {
        // Đặt đạn và tường trùng vị trí va chạm
        Vector2D collisionPos = new Vector2D(150.0, 150.0);
        MockObstacleWall wall = new MockObstacleWall("wall_1", collisionPos);
        gameEngine.getGameState().getEnvironments().add(wall);

        Bullet bullet = new Bullet("bullet_wall", playerId, collisionPos, Direction.RIGHT, Bullet.SPEED_NORMAL, 1, 0);
        gameEngine.getGameState().addBullet(bullet);

        gameEngine.update();

        assertTrue(wall.isHitCalled(), "Tường phải gọi hàm takeHit khi đạn đâm vào");
        assertEquals(0, wall.getLastStarLevel(), "Cấp độ sao truyền vào takeHit phải khớp với starLevel của đạn");
        assertTrue(gameEngine.getGameState().getBullets().isEmpty(), "Viên đạn phải bị xóa sau khi chạm tường");
    }

    @Test
    @DisplayName("TC04: Đạn xuyên qua môi trường cho phép (canBulletPass = true) mà không bị xóa")
    void testTC04_BulletPassesThroughPassableEnvironment() {
        Vector2D passPos = new Vector2D(150.0, 150.0);
        MockPassableTile tile = new MockPassableTile("grass_1", passPos);
        gameEngine.getGameState().getEnvironments().add(tile);

        Bullet bullet = new Bullet("bullet_pass", playerId, passPos, Direction.RIGHT, Bullet.SPEED_SLOW, 1, 0);
        gameEngine.getGameState().addBullet(bullet);

        gameEngine.update();

        assertFalse(gameEngine.getGameState().getBullets().isEmpty(), "Đạn phải đi xuyên qua cỏ/nước và tiếp tục tồn tại");
    }

    @Test
    @DisplayName("TC01: Đạn bắn trúng Xe tăng đối phương sẽ gây sát thương và xóa đạn")
    void testTC01_BulletHitsEnemyTank() {
        String enemyId = "enemy_player";
        Vector2D enemyPos = new Vector2D(300.0, 300.0);
        gameEngine.spawnPlayers(List.of(enemyId), List.of(enemyPos));

        Tank enemyTank = gameEngine.getGameState().getTankById(enemyId);
        int initialHealth = enemyTank.getHealth();

        // Đặt đạn của player_1 trùng vị trí enemyTank
        Bullet bullet = new Bullet("bullet_hit", playerId, enemyPos, Direction.RIGHT, Bullet.SPEED_NORMAL, 1, 0);
        gameEngine.getGameState().addBullet(bullet);

        gameEngine.update();

        assertTrue(enemyTank.getHealth() < initialHealth, "Máu của xe tăng đối phương phải bị giảm khi trúng đạn");
        assertTrue(gameEngine.getGameState().getBullets().isEmpty(), "Đạn bắn trúng đối phương phải bị xóa");
    }

    @Test
    @DisplayName("Đạn KHÔNG gây sát thương lên chính người bắn (Owner)")
    void testBulletDoesNotHitShooterSelf() {
        Tank shooterTank = gameEngine.getGameState().getTankById(playerId);
        int initialHealth = shooterTank.getHealth();

        // Đặt đạn ngay vị trí của chính người bắn (ownerId = playerId)
        Bullet bullet = new Bullet("bullet_self", playerId, startPos, Direction.UP, Bullet.SPEED_NORMAL, 1, 0);
        gameEngine.getGameState().addBullet(bullet);

        gameEngine.update();

        assertEquals(initialHealth, shooterTank.getHealth(), "Người bắn không bị mất máu bởi đạn của chính mình");
    }

    @Test
    @DisplayName("Đạn bắn trúng làm máu xe tăng về 0 -> Xe bị đánh dấu isDestroyed và bị xóa khỏi GameState")
    void testTankDestroyedByBulletDamageIsCleanedUp() {
        String enemyId = "enemy_target";
        Vector2D enemyPos = new Vector2D(200.0, 200.0);
        gameEngine.spawnPlayers(List.of(enemyId), List.of(enemyPos));

        Tank enemyTank = gameEngine.getGameState().getTankById(enemyId);
        assertNotNull(enemyTank);

        // Tạo viên đạn có sát thương đúng bằng máu tối đa của Xe tăng (Chí mạng)
        int lethalDamage = enemyTank.getMaxHealth();
        Bullet lethalBullet = new Bullet("bullet_fatal", playerId, enemyPos, Direction.RIGHT, Bullet.SPEED_NORMAL, lethalDamage, 0);
        gameEngine.getGameState().addBullet(lethalBullet);

        // Chạy nhịp tick update()
        gameEngine.update();

        // 1. Kiểm tra xe đã nhận sát thương qua takeDamage làm health về <= 0
        assertTrue(enemyTank.isDestroyed(), "Xe tăng phải rơi vào trạng thái destroyed (health <= 0)");

        // 2. Kiểm tra cleanupDestroyedTanks đã dọn dẹp và xóa xe khỏi GameState
        assertNull(gameEngine.getGameState().getTankById(enemyId), "Xe bị bắn hết máu phải bị xóa hoàn toàn khỏi GameState");
        assertTrue(gameEngine.getGameState().getBullets().isEmpty(), "Viên đạn gây sát thương cũng phải bị xóa");
    }

    @Test
    @DisplayName("TC05: PlayerTank bị bắn hết máu nhưng còn mạng (lives > 0) thì được respawn tại spawnPoint và không bị xóa khỏi GameState")
    void testTC05_PlayerTankRespawnsWhenLivesRemain() {
        PlayerTank player = (PlayerTank) gameEngine.getGameState().getTankById(playerId);
        assertNotNull(player, "PlayerTank phải tồn tại ban đầu");

        // 1. Giả lập người chơi ăn PowerUp nâng cấp (lives = 2, starLevel = 1)
        player.addLife(); // Mạng tăng từ 1 lên 2
        player.upgradeStar(); // Nâng cấp sao lên level 1

        // Đổi vị trí xe sang chỗ khác để kiểm tra xem respawn có đưa về spawnPoint ban đầu hay không
        Vector2D movedPos = new Vector2D(200.0, 200.0);
        player.setPosition(movedPos);

        assertEquals(2, player.getLives(), "Số mạng ban đầu sau khi thêm phải là 2");
        assertEquals(1, player.getStarLevel(), "Cấp độ sao ban đầu là 1");

        // 2. Tạo đạn trúng xe tăng
        Bullet bullet = new Bullet("bullet_hit", "enemy_bot", movedPos, Direction.DOWN, Bullet.SPEED_NORMAL, 1, 0);
        gameEngine.getGameState().addBullet(bullet);

        // 3. Chạy nhịp tick update()
        gameEngine.update();

        // 4. Kiểm tra trạng thái sau khi dính đạn và respawn:
        // - Số mạng giảm từ 2 xuống 1
        assertEquals(1, player.getLives(), "Số mạng phải giảm từ 2 xuống 1");

        // - Máu được hồi phục lại maxHealth (1)
        assertEquals(player.getMaxHealth(), player.getHealth(), "Máu phải được hồi lại đầy (1) sau khi respawn");

        // - Cấp độ sao bị reset về 0
        assertEquals(0, player.getStarLevel(), "Cấp độ sao phải bị reset về 0 sau khi bị hạ gục");

        // - Vị trí xe được đưa trở lại spawnPoint ban đầu (startPos)
        assertEquals(startPos.x(), player.getPosition().x(), "Tọa độ X phải về lại spawnPoint ban đầu");
        assertEquals(startPos.y(), player.getPosition().y(), "Tọa độ Y phải về lại spawnPoint ban đầu");

        // - Xe chưa bị tiêu diệt hoàn toàn và vẫn còn nguyên trong GameState
        assertFalse(player.isDestroyed(), "Xe chưa bị destroyed vì vẫn còn 1 mạng");
        assertNotNull(gameEngine.getGameState().getTankById(playerId), "Xe vẫn phải tồn tại trong GameState");

        // - Viên đạn gây sát thương đã bị xóa
        assertTrue(gameEngine.getGameState().getBullets().isEmpty(), "Đạn sau khi trúng phải bị xóa khỏi GameState");
    }

    // =========================================================================
    // TEST CASES FOR ON_KILL_CALLBACK & GAME_OVER LOGIC
    // =========================================================================

    @Test
    @DisplayName("onKillCallback gửi đúng ID người bắn khi tiêu diệt EnemyTank (Bot)")
    void testOnKillCallbackTriggersOnEnemyTankDestroyed() {
        String enemyBotId = "bot_1";
        Vector2D botPos = new Vector2D(300.0, 300.0);
        gameEngine.spawnBots(List.of(enemyBotId), List.of(botPos));

        // Tạo danh sách hứng killerId trả về từ callback
        List<String> killedByList = new ArrayList<>();
        gameEngine.setOnKillCallback(killedByList::add);

        Tank enemyBot = gameEngine.getGameState().getTankById(enemyBotId);
        assertNotNull(enemyBot);

        // Bắn viên đạn chí mạng vào Bot
        Bullet lethalBullet = new Bullet("bullet_fatal_bot", playerId, botPos, Direction.RIGHT, Bullet.SPEED_NORMAL, enemyBot.getMaxHealth(), 0);
        gameEngine.getGameState().addBullet(lethalBullet);

        gameEngine.update();

        // Kiểm tra callback đã nhận đúng playerId của người bắn
        assertEquals(1, killedByList.size(), "Callback phải được gọi đúng 1 lần khi Bot nổ");
        assertEquals(playerId, killedByList.get(0), "ID của killer truyền sang callback phải là playerId");
    }

    @Test
    @DisplayName("onKillCallback vẫn gửi đúng killerId khi PlayerTank bị bắn nổ mất 1 mạng (dù chưa chết hẳn)")
    void testOnKillCallbackTriggersOnPlayerTankLosesLife() {
        String enemyPlayerId = "player_2";
        Vector2D enemyPos = new Vector2D(400.0, 400.0);
        gameEngine.spawnPlayers(List.of(enemyPlayerId), List.of(enemyPos));

        PlayerTank targetPlayer = (PlayerTank) gameEngine.getGameState().getTankById(enemyPlayerId);
        assertNotNull(targetPlayer);

        // Thêm mạng để Player 2 có 2 mạng
        targetPlayer.addLife();
        assertEquals(2, targetPlayer.getLives());

        List<String> killedByList = new ArrayList<>();
        gameEngine.setOnKillCallback(killedByList::add);

        // player_1 bắn trúng làm player_2 nổ mất 1 mạng
        Bullet bullet = new Bullet("bullet_hit_p2", playerId, enemyPos, Direction.RIGHT, Bullet.SPEED_NORMAL, targetPlayer.getMaxHealth(), 0);
        gameEngine.getGameState().addBullet(bullet);

        gameEngine.update();

        // Mạng giảm xuống 1
        assertEquals(1, targetPlayer.getLives());
        assertFalse(targetPlayer.isDestroyed(), "Player 2 chưa bị tiêu diệt hoàn toàn vì vẫn còn 1 mạng");

        // Callback vẫn phải ghi nhận player_1 đã có 1 lượt hạ gục (kill)
        assertEquals(1, killedByList.size(), "Callback phải được kích hoạt ngay cả khi chỉ làm đối phương mất 1 mạng");
        assertEquals(playerId, killedByList.get(0), "Killer ID ghi nhận phải là player_1");
    }

    @Test
    @DisplayName("GameState được cập nhật isGameOver = true khi số xe còn sống <= 1")
    void testGameOverSetToTrueWhenOneOrZeroTanksRemain() {
        // Ban đầu trong setUp chỉ spawn đúng 1 xe (player_1)
        assertFalse(gameEngine.getGameState().isGameOver(), "Game chưa over khi vừa khởi tạo");

        // Chạy update tick đầu tiên
        gameEngine.update();

        // Vì chỉ có 1 xe trên map (aliveCount = 1) -> Game Engine tự nhận biết và setGameOver(true)
        assertTrue(gameEngine.getGameState().isGameOver(), "GameState phải chuyển isGameOver = true do chỉ còn <= 1 xe sống");
    }

    @Test
    @DisplayName("Game không over khi còn >= 2 xe sống, và tự động over khi 1 xe bị tiêu diệt")
    void testGameOverTriggersOnlyWhenTanksReducedToOne() {
        String enemyId = "enemy_player";
        Vector2D enemyPos = new Vector2D(300.0, 300.0);
        gameEngine.spawnPlayers(List.of(enemyId), List.of(enemyPos));

        Tank enemyTank = gameEngine.getGameState().getTankById(enemyId);

        // Update nhịp tick 1: Trên map đang có 2 xe (player_1 và enemy_player)
        gameEngine.update();
        assertFalse(gameEngine.getGameState().isGameOver(), "Game không được over khi vẫn còn 2 xe sống");

        // Bắn nổ hoàn toàn enemy_player
        Bullet lethalBullet = new Bullet("bullet_fatal", playerId, enemyPos, Direction.RIGHT, Bullet.SPEED_NORMAL, enemyTank.getMaxHealth(), 0);
        gameEngine.getGameState().addBullet(lethalBullet);

        // Update nhịp tick 2: enemy_player bị nổ và dọn dẹp khỏi GameState -> Số xe sống giảm về 1
        gameEngine.update();

        assertTrue(gameEngine.getGameState().isGameOver(), "Game phải lập tức chuyển isGameOver = true sau khi 1 xe bị tiêu diệt làm số xe còn lại <= 1");
    }

    // =========================================================================
    // TEST CASES FOR SHOOT COOLDOWN LOGIC
    // =========================================================================

    @Test
    @DisplayName("COOLDOWN: Bắn 2 lần liên tiếp ngay lập tức chỉ tạo ra đúng 1 viên đạn")
    void testShootCooldown_SpamShootBlocked() {
        // Lần bắn thứ 1: Thành công
        gameEngine.handleShootEvent(playerId);
        assertEquals(1, gameEngine.getGameState().getBullets().size(), "Lần bắn đầu tiên phải tạo ra 1 viên đạn");

        // Lần bắn thứ 2 ngay lập tức (0ms trôi qua): Bị chặn bởi Cooldown
        gameEngine.handleShootEvent(playerId);
        assertEquals(1, gameEngine.getGameState().getBullets().size(), "Lần bắn thứ 2 liên tiếp phải bị block do chưa hết Cooldown");
    }

    @Test
    @DisplayName("COOLDOWN: Có thể bắn tiếp viên thứ 2 sau khi đã chờ đủ thời gian Cooldown")
    void testShootCooldown_CanShootAfterCooldownPasses() throws InterruptedException {
        Tank tank = gameEngine.getGameState().getTankById(playerId);
        assertNotNull(tank);
        tank.setShootCooldownMillis(100); // Đặt cooldown ngắn 100ms để test nhanh

        // Lần bắn 1
        gameEngine.handleShootEvent(playerId);
        assertEquals(1, gameEngine.getGameState().getBullets().size());

        // Giả lập chờ 120ms (đã vượt qua cooldown 100ms)
        Thread.sleep(120);

        // Lần bắn 2: Thành công
        gameEngine.handleShootEvent(playerId);
        assertEquals(2, gameEngine.getGameState().getBullets().size(), "Phải bắn được viên đạn thứ 2 sau khi đã chờ hết Cooldown");
    }


    @Test
    @DisplayName("COOLDOWN: Các người chơi khác nhau có bộ đếm Cooldown độc lập")
    void testShootCooldown_IndependentBetweenPlayers() {
        String player2Id = "player_2";
        gameEngine.spawnPlayers(List.of(player2Id), List.of(new Vector2D(200.0, 200.0)));

        // Player 1 bắn
        gameEngine.handleShootEvent(playerId);
        assertEquals(1, gameEngine.getGameState().getBullets().size());

        // Player 2 bắn ngay lập tức -> Vẫn thành công vì Cooldown tính riêng cho từng xe
        gameEngine.handleShootEvent(player2Id);
        assertEquals(2, gameEngine.getGameState().getBullets().size(), "Player 2 phải bắn được độc lập với Cooldown của Player 1");

        // Player 1 bắn tiếp ngay -> Bị block
        gameEngine.handleShootEvent(playerId);
        assertEquals(2, gameEngine.getGameState().getBullets().size());
    }





}