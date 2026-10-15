package tankops.common.engine;

import tankops.common.map.GameMap;
import tankops.common.model.common.Direction;
import tankops.common.model.common.Vector2D;
import tankops.common.model.environment.Environment;
import tankops.common.model.tank.*;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class GameEngine {

    private final GameState gameState = new GameState();
    private final Random random = new Random();
    private final Map<String, Direction> playerMoveDirections = new ConcurrentHashMap<>();


    //Initialize GameEngine with populating environment data from GameMap into GameState
    public GameEngine(GameMap gameMap) {
        if(gameMap != null && gameMap.getEnvironments() != null){
            this.gameState.setEnvironments(gameMap.getEnvironments());
        }
    }

    //Spawn player tanks
    public void spawnPlayers(List<String> playerIds, List<Vector2D> playerSpawnPoints){
        if(playerIds == null || playerSpawnPoints == null || playerSpawnPoints.isEmpty()) {
            return;
        }

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

    public void handleMoveEvent(String playerId, Direction direction) {
        if (direction == null) {
            playerMoveDirections.remove(playerId);
        } else {
            playerMoveDirections.put(playerId, direction);
        }
    }
    //update game state per tick
    public void update(){

        updatePlayerTanks();
    }
    private void updatePlayerTanks() {
        playerMoveDirections.forEach((playerId, direction) -> {
            Tank tank = this.gameState.getTankById(playerId);
            if (tank != null && !tank.isDestroyed()) {
                Vector2D oldPosition = tank.getPosition();
                tank.move(direction); // update both dir and pos

                if(checkWallCollision(tank)){
                    tank.setPosition(oldPosition);
                }
            }
        });
    }

    private boolean checkWallCollision(Tank tank){
        for(Environment env : this.gameState.getEnvironments()){
            if(!env.canTankPass() && tank.intersects(env)){
                return true;
            }
        }

        return false;

    }


    //get current game state
    public GameState getGameState() {
        return this.gameState;
    }


}
