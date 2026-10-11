package tankops.common.map;

import tankops.common.model.common.Vector2D;
import tankops.common.model.environment.Environment;

import java.util.List;

public class GameMap {
    private final List<Environment> environments;
    private final List<Vector2D> playerSpawnPoints;
    private final List<Vector2D> enemySpawnPoints;

    public GameMap(List<Environment> environments, List<Vector2D> playerSpawnPoints, List<Vector2D> enemySpawnPoints) {
        this.environments = environments;
        this.playerSpawnPoints = playerSpawnPoints;
        this.enemySpawnPoints = enemySpawnPoints;
    }

    public List<Environment> getEnvironments() {
        return environments;
    }
    public List<Vector2D> getPlayerSpawnPoints() {
        return playerSpawnPoints;

    }
    public List<Vector2D> getEnemySpawnPoints() {
        return enemySpawnPoints;

    }

}
