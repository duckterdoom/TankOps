package tankops.common.map;

import tankops.common.model.common.Vector2D;
import tankops.common.model.environment.Environment;

import java.util.List;

public class GameMap {
    private final List<Environment> environments;
    private final List<Vector2D> playerSpawnPoints;
    private final List<Vector2D> enemySpawnPoints;

    private final int cols;
    private final int rows;

    public GameMap(List<Environment> environments, List<Vector2D> playerSpawnPoints, List<Vector2D> enemySpawnPoints,  int cols, int rows) {
        this.environments = environments;
        this.playerSpawnPoints = playerSpawnPoints;
        this.enemySpawnPoints = enemySpawnPoints;
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

    public int getCols(){
        return cols;
    }
    public int getRows(){
        return rows;
    }


}
