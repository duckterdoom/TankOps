package tankops.common.engine;

import tankops.common.map.GameMap;
import tankops.common.model.common.Bullet;
import tankops.common.model.common.Direction;
import tankops.common.model.common.Vector2D;
import tankops.common.model.environment.Environment;
import tankops.common.model.environment.Ice;
import tankops.common.model.powerups.*;
import tankops.common.model.powerups.Timer;
import tankops.common.model.tank.*;
import tankops.common.model.tank.state.AttackState;
import tankops.common.model.tank.state.ChaseState;
import tankops.common.model.tank.state.PatrolState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GameEngine {

    private final GameState gameState = new GameState();
    private final Random random = new Random();
    private final Map<String, Direction> playerMoveDirections = new ConcurrentHashMap<>();
    private final List<Vector2D> powerUpSpawnPoints;

    private double mapWidth = 800.0;
    private double mapHeight = 600.0;

    private int powerUpSpawnTimer = 0;
    private static final int SPAWN_INTERVAL_TICKS = 400;


    // Callback dùng để báo cho GameRoom biết khi có player hạ gục tank khác
    private Consumer<String> onKillCallback;
    //Initialize GameEngine with populating environment data from GameMap into GameState
    public GameEngine(GameMap gameMap) {
        if(gameMap != null ){
            if(gameMap.getEnvironments() != null){
                this.gameState.setEnvironments(gameMap.getEnvironments());
            }
            if(gameMap.getPowerUpSpawnPoints() != null){
                this.powerUpSpawnPoints = gameMap.getPowerUpSpawnPoints();
            }else {
                this.powerUpSpawnPoints = new ArrayList<>();
            }
            if (gameMap.getCols() > 0 && gameMap.getRows() > 0) {
                this.mapWidth = gameMap.getCols() * Environment.TILE_SIZE;
                this.mapHeight = gameMap.getRows() * Environment.TILE_SIZE;
            }
        }else{
            this.powerUpSpawnPoints = new ArrayList<>();
        }

    }
    // Đăng ký callback để gửi sự kiện Kill về cho GameRoom (không làm thay đổi logic GameRoom)
    public void setOnKillCallback(Consumer<String> onKillCallback) {
        this.onKillCallback = onKillCallback;
    }

    //Spawn player tanks
    public void spawnPlayers(List<String> playerIds, List<Vector2D> playerSpawnPoints){
        if(playerIds == null || playerSpawnPoints == null || playerSpawnPoints.isEmpty()) {
            return;
        }

        // Prevents IndexOutOfBoundsException if the number of player ids exceeds available spawn points
        int count = Math.min(playerIds.size(), playerSpawnPoints.size());

        for (int i = 0; i < count; i++) {
            int playerNumber = i + 1;
            PlayerTank tank = new PlayerTank(
                    playerIds.get(i),
                    playerSpawnPoints.get(i),
                    Direction.UP,
                    playerNumber
            );

            this.gameState.addTank(tank);

        }
    }

    //spawn bot tanks
    public void spawnBots(List<String> botIds, List<Vector2D> botSpawnPoints){
        if(botIds == null || botSpawnPoints == null || botSpawnPoints.isEmpty()) {
            return;
        }
        int count = Math.min(botIds.size(), botSpawnPoints.size());
        for (int i = 0; i < count; i++) {
            EnemyTank bot = createRandomBot(botIds.get(i), botSpawnPoints.get(i));
            this.gameState.addTank(bot);
        }
    }

    //helper method
    private EnemyTank createRandomBot(String botId, Vector2D spawnPos){
        int type = random.nextInt(4);
        return switch (type){
            case 0 -> new BasicTank(botId, spawnPos, Direction.DOWN);
            case 1 -> new FastTank(botId, spawnPos, Direction.DOWN);
            case 2 -> new PowerTank(botId, spawnPos, Direction.DOWN);
            case 3 -> new ArmorTank(botId, spawnPos, Direction.DOWN);
            default -> new BasicTank(botId, spawnPos, Direction.DOWN);

        };
    }

    private void spawnPowerUp() {
        if (powerUpSpawnPoints == null || powerUpSpawnPoints.isEmpty()) {
            return;
        }

        powerUpSpawnTimer++;
        if (powerUpSpawnTimer >= SPAWN_INTERVAL_TICKS) {
            powerUpSpawnTimer = 0; // Reset bộ đếm

            // Chọn ngẫu nhiên một tọa độ từ danh sách các điểm spawn của map
            Vector2D spawnPos = powerUpSpawnPoints.get(random.nextInt(powerUpSpawnPoints.size()));

            String powerUpId = "powerup_" + System.currentTimeMillis();


            PowerUp powerUp = createRandomPowerUp(powerUpId, spawnPos);

            // add powerup to gamestate
            if (powerUp != null) {
                this.gameState.addPowerUp(powerUp);
            }
        }
    }

    // Hàm phụ trợ tạo ngẫu nhiên các loại PowerUp
    private PowerUp createRandomPowerUp(String id, Vector2D position) {
        int type = random.nextInt(6);

        return switch (type) {
            case 0 -> new Grenade(id, position);
            case 1 -> new Helmet(id, position);
            case 2 -> new Shovel(id, position);
            case 3 -> new Star(id, position);
            case 4 -> new TankPowerUp(id, position);
            default -> new Timer(id, position);
        };
    }

    public void handleMoveEvent(String playerId, Direction direction) {
        if (direction == null) {
            playerMoveDirections.remove(playerId);
        } else {
            playerMoveDirections.put(playerId, direction);
        }
    }


    public void handleShootEvent(String playerId) {

        if (playerId == null || playerId.isBlank()) {
            return;
        }

        Tank tank = this.gameState.getTankById(playerId);

        // check whether tank is destroyed or not
        if (tank != null && !tank.isDestroyed() && tank.canShoot()) {
            //create bulletId based on playerId
            String bulletId = playerId + "_b_" + System.currentTimeMillis();

            //create bullet
            Bullet bullet = tank.fire(bulletId);

            // add bullet to game state
            this.gameState.addBullet(bullet);
        }
    }
    //update game state per tick
    public void update(){

        updatePlayerTanks();
        updateBots();
        updateBullets();
        spawnPowerUp();
        checkPowerUpCollisions();
        checkGameOverCondition();
        cleanupDestroyedTanks();
    }
    private void updatePlayerTanks() {
        for(Tank tank : this.gameState.getTanks()){
            tank.setMoving(false);

            if (tank instanceof PlayerTank playerTank && !playerTank.isDestroyed()) {
                playerTank.updateShield();
            }
        }

        playerMoveDirections.forEach((playerId, direction) -> {
            Tank tank = this.gameState.getTankById(playerId);
            if (tank != null && !tank.isDestroyed()) {
//                Vector2D oldPosition = tank.getPosition();
                Vector2D oldPosition = new Vector2D(tank.getPosition().x(), tank.getPosition().y());
                //when moving if detect collision then rollback pos
                tank.move(direction); // update both dir and pos

                if(!tank.isWithinBounds(mapWidth, mapHeight) || checkWallCollision(tank)){
                    tank.setPosition(oldPosition);
                }else {
                    tank.setMoving(true);

                    // ĐANG GIỮ PHÍM VÀ Ở TRÊN BĂNG -> KÍCH HOẠT ĐÀ TRƯỢT
                    double slideDist = getIceSlideDistance(tank);
                    if (slideDist > 0) {
                        tank.triggerSlide(slideDist);
                    }
                }
            }

        });
        // 2. XỬ LÝ QUÁN TÍNH TRƯỢT (Áp dụng cho cả khi đang giữ phím HOẶC KHI ĐÃ THẢ PHÍM)
        for (Tank tank : this.gameState.getTanks()) {
            if (tank != null && !tank.isDestroyed()) {

                // Bổ sung kiểm tra: Nếu người chơi ĐÃ THẢ PHÍM (không nằm trong playerMoveDirections)
                // nhưng vẫn đang đứng trên băng, ta kích hoạt đà trượt theo hướng mặt hiện tại của tank (direction)
                if (!playerMoveDirections.containsKey(tank.getId())) {
                    double slideDist = getIceSlideDistance(tank);
                    if (slideDist > 0 && !tank.isSliding()) {
                        tank.triggerSlide(slideDist);
                    }
                }

                // Tiến hành thực hiện trượt nếu còn quãng đường trượt (slideDistanceRemaining > 0)
                if (tank.isSliding()) {
                    Vector2D oldPosition = new Vector2D(tank.getPosition().x(), tank.getPosition().y());
                    tank.slideForward(); // Thực hiện 1 bước trượt

                    if (!tank.isWithinBounds(mapWidth, mapHeight) || checkWallCollision(tank)) {
                        tank.setPosition(oldPosition); // Đụng tường -> Rollback & ngắt trượt
                        tank.triggerSlide(0);
                    } else {
                        tank.setMoving(true);
                    }
                }
            }
        }


    }
    private double getIceSlideDistance(Tank tank) {
        for (Environment env : this.gameState.getEnvironments()) {
            if (env instanceof Ice ice && ice.isSlippery() && tank.intersects(ice)) {
                return ice.getSlideDistance();
            }
        }
        return 0.0;
    }
    private void updateBullets(){
        Iterator<Bullet> iterator = this.gameState.getBullets().iterator();

        while (iterator.hasNext()) {
            Bullet bullet = iterator.next();


            bullet.move();

            if (!bullet.isWithinBounds(mapWidth, mapHeight)) {
                iterator.remove();
                continue;
            }

            if(checkBulletWallCollision(bullet)){
                iterator.remove();
                continue;
            }

            if(checkBulletTankCollision(bullet)){
                iterator.remove();
            }
        }

    }

    private boolean checkWallCollision(Tank tank){
        for(Environment env : this.gameState.getEnvironments()){
            if(!env.canTankPass() && tank.intersects(env)){
                return true;
            }
        }

        return false;

    }

    private boolean checkBulletWallCollision(Bullet bullet){
        Iterator<Environment> iterator = this.gameState.getEnvironments().iterator();

        while (iterator.hasNext()) {
            Environment env = iterator.next();

            // Bỏ qua nếu môi trường cho đạn đi xuyên qua (cỏ, nước...)
            if (env.canBulletPass()) {
                continue;
            }

            // Nếu chạm vào tường cản đạn
            if (bullet.intersects(env)) {
                env.takeHit(bullet.getStarLevel());

                // Nếu tường bị phá hủy hoàn toàn -> Xóa khỏi GameState an toàn bằng Iterator
                if (env.isDestroyed()) {
                    iterator.remove();
                }

                return true; // Trả về true để xóa đạn
            }
        }

        return false;
    }

    private boolean checkBulletTankCollision(Bullet bullet) {
        for (Tank tank : this.gameState.getTanks()) {
            // Bỏ qua xe tăng đã bị tiêu diệt hoặc chính xe tăng đã bắn ra viên đạn này
            if (tank.isDestroyed() || tank.getId().equals(bullet.getOwnerId())) {
                continue;
            }

            // Nếu đạn chạm vào xe tăng
            if (bullet.intersects(tank)) {
                // Lưu lại trạng thái số mạng hoặc trạng thái sống trước khi nhận sát thương
                int oldLives = (tank instanceof PlayerTank playerTank) ? playerTank.getLives() : 1;

                // Xe chịu sát thương (bên trong hàm này sẽ tự trừ health/lives)
                tank.takeDamage(bullet.getDamage());

                int newLives = (tank instanceof PlayerTank playerTank) ? playerTank.getLives() : 0;

                // TÍNH ĐIỂM KHI:
                // 1. Xe bị phá hủy hoàn toàn (isDestroyed)
                // HOẶC 2. Player Tank bị nổ mất 1 mạng (oldLives > newLives)
                boolean wasKilled = tank.isDestroyed() || (oldLives > newLives);

                if (wasKilled) {
                    String killerId = bullet.getOwnerId();
                    if (killerId != null && onKillCallback != null) {
                        onKillCallback.accept(killerId); // Báo về GameRoom để addKill
                    }
                }
                return true; // Trả về true để xóa đạn
            }
        }
        return false;
    }
    private void checkPowerUpCollisions() {
        if (this.gameState.getPowerUps() == null || this.gameState.getPowerUps().isEmpty()) {
            return;
        }

        Iterator<PowerUp> powerUpIterator = this.gameState.getPowerUps().iterator();

        while (powerUpIterator.hasNext()) {
            PowerUp powerUp = powerUpIterator.next();

            // Duyệt qua tất cả các xe tăng đang sống trên map
            for (Tank tank : this.gameState.getTanks()) {
                if (tank.isDestroyed()) {
                    continue;
                }

                // Kiểm tra nếu xe tăng va chạm với power-up
                if (tank.intersects(powerUp)) {

                    // Nếu xe tăng là PlayerTank thì kích hoạt hiệu ứng của item
                    if (tank instanceof PlayerTank playerTank) {
                        powerUp.applyEffect(playerTank, this.gameState);

                        // Xóa power-up khỏi danh sách (vật phẩm biến mất sau khi bị ăn)
                        powerUpIterator.remove();
                        break;
                    }


                }
            }
        }
    }

    private void updateBots() {
        for (Tank tank : this.gameState.getTanks()) {
            if (tank instanceof EnemyTank bot && !bot.isDestroyed()) {
                // Cập nhật trạng thái đóng băng của đồng hồ Timer mỗi tick
                bot.updateFreeze();

                // Nếu bot đang bị đóng băng -> Bỏ qua toàn bộ logic di chuyển và tấn công
                if (bot.isFrozen()) {
                    bot.setMoving(false);
                    continue;
                }

                PlayerTank targetPlayer = findNearestPlayer(bot);
                double distance = (targetPlayer != null) ? calculateDistance(bot.getPosition(), targetPlayer.getPosition()) : Double.MAX_VALUE;

                // CHUYỂN TRẠNG THÁI SỬ DỤNG SINGLETON

                if (targetPlayer == null || distance > 350.0) {
                    if (bot.getCurrentState() != PatrolState.getInstance()) bot.changeState(PatrolState.getInstance());
                } else if (distance > 150) {
                    if (bot.getCurrentState() != ChaseState.getInstance()) bot.changeState(ChaseState.getInstance());
                } else {
                     bot.changeState(AttackState.getInstance());
                }
                // THỰC THI
                bot.getCurrentState().execute(bot, this);
            }
        }
    }

    // --- CÁC PHƯƠNG THỨC HELPER CHO AI & TOÁN HỌC ---

    public double calculateDistance(Vector2D p1, Vector2D p2) {
        return Math.hypot(p2.x() - p1.x(), p2.y() - p1.y());
    }

    public PlayerTank findNearestPlayer(EnemyTank bot) {
        PlayerTank nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Tank tank : this.gameState.getTanks()) {
            if (tank instanceof PlayerTank player && !player.isDestroyed()) {
                double dist = calculateDistance(bot.getPosition(), player.getPosition());
                if (dist < minDistance) {
                    minDistance = dist;
                    nearest = player;
                }
            }
        }
        return nearest;
    }


    public Direction getRandomDirection() {
        Direction[] dirs = Direction.values();
        return dirs[random.nextInt(dirs.length)];
    }

    public boolean moveBot(Tank tank, Direction direction) {
        Vector2D old = new Vector2D(tank.getPosition().x(), tank.getPosition().y());
        tank.setDirection(direction);
        tank.move(direction);
        if (!tank.isWithinBounds(mapWidth, mapHeight) || checkWallCollision(tank)) {
            tank.setPosition(old);
            tank.setMoving(false);
            return false;
        }
        tank.setMoving(true);
        return true;
    }

    private void cleanupDestroyedTanks() {
        Iterator<Tank> iterator = this.gameState.getTanks().iterator();
        while (iterator.hasNext()) {
            Tank tank = iterator.next();
            if (tank.isDestroyed()) {
                iterator.remove(); // Xóa tank đã chết khỏi GameState
            }
        }
    }

    // XỬ LÝ END GAME:
    // Tự động kiểm tra số xe còn sống để setGameOver trong GameState
    private void checkGameOverCondition() {
        int aliveCount = 0;
        for (Tank tank : this.gameState.getTanks()) {
            if (!tank.isDestroyed()) {
                aliveCount++;
            }
        }
        // Nếu số xe sống <= 1 thì cập nhật isGameOver trong GameState
        if (aliveCount <= 1) {
            this.gameState.setGameOver(true);
        }
    }

    public double getMapHeight() {
        return mapHeight;
    }
    public double getMapWidth() {
        return mapWidth;
    }

    //get current game state
    public GameState getGameState() {
        return this.gameState;
    }



}
