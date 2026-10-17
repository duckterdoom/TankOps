package tankops.client.controller;

import tankops.common.engine.GameEngine;
import tankops.common.engine.GameState;
import tankops.common.map.GameMap;
import tankops.common.map.MapLoader;
import tankops.common.model.common.Direction;

import java.util.List;

public class SinglePlayerController {
    private GameEngine engine;
    private final String localPlayerId = "PLAYER_1";
    private final String botPlayerId = "BOT_E1";

    public void initGame() {
        try {
            MapLoader mapLoader = new MapLoader();
            GameMap gameMap = mapLoader.loadMap("/maps/stage1.map");

            if (gameMap != null) {
                engine = new GameEngine(gameMap);
                engine.spawnPlayers(List.of(localPlayerId), gameMap.getPlayerSpawnPoints());
                engine.spawnBots(List.of(botPlayerId), gameMap.getEnemySpawnPoints());
            }
        } catch (Exception e) {
            System.err.println("[SinglePlayerController] Error loading map: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void handleInput(boolean isUp, boolean isDown, boolean isLeft, boolean isRight, boolean isShooting) {
        if (engine == null) return;

        Direction moveDirection = null;
        if (isUp) {
            moveDirection = Direction.UP;
        } else if (isDown) {
            moveDirection = Direction.DOWN;
        } else if (isLeft) {
            moveDirection = Direction.LEFT;
        } else if (isRight) {
            moveDirection = Direction.RIGHT;
        }

        engine.handleMoveEvent(localPlayerId, moveDirection);

        // TODO: Gọi hàm handleShootEvent(localPlayerId) khi đồng đội bổ sung tính năng bắn vào GameEngine
    }

    public void update() {
        if (engine != null) {
            engine.update();
        }
    }

    public GameState getGameState() {
        return engine != null ? engine.getGameState() : null;
    }

    public String getLocalPlayerId() {
        return localPlayerId;
    }
}
