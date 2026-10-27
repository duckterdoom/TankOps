package tankops.common.map;

import tankops.common.model.common.Vector2D;
import tankops.common.model.environment.Environment;

import java.util.List;

public class GameMap {
    private final List<Environment> environments;
    private final List<Vector2D> playerSpawnPoints;
    private final List<Vector2D> enemySpawnPoints;
    private final List<Vector2D> powerUpSpawnPoints;

    private final int cols;
    private final int rows;

    public GameMap(List<Environment> environments, List<Vector2D> playerSpawnPoints, List<Vector2D> enemySpawnPoints, List<Vector2D> powerUpSpawnPoints,  int cols, int rows) {
        this.environments = environments;
        this.playerSpawnPoints = playerSpawnPoints;
        this.enemySpawnPoints = enemySpawnPoints;
        this.powerUpSpawnPoints = powerUpSpawnPoints;
        this.cols = cols;
        this.rows = rows;

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
    public List<Vector2D> getPowerUpSpawnPoints() {
        return powerUpSpawnPoints;
    }

    public int getCols(){
        return cols;
    }
    public int getRows(){
        return rows;
    }


}
