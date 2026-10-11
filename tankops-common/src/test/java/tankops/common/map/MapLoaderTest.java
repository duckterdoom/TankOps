package tankops.common.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tankops.common.model.common.Vector2D;
import tankops.common.model.environment.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MapLoaderTest {

    private MapLoader mapLoader;

    @BeforeEach
    void setUp() {
        mapLoader = new MapLoader();
    }

    @Test
    void testLoadMap_Success() {
        GameMap map = mapLoader.loadMap("/maps/stage1.map");

        assertNotNull(map);
        assertNotNull(map.getEnvironments());
        assertFalse(map.getEnvironments().isEmpty());

        Environment first = map.getEnvironments().get(0);
        assertNotNull(first.getId());
        assertNotNull(first.getPosition());
    }

    @Test
    void testLoadMap_SpawnPoints() {
        GameMap map = mapLoader.loadMap("/maps/stage1.map");


        assertNotNull(map.getPlayerSpawnPoints());
        assertNotNull(map.getEnemySpawnPoints());


        assertFalse(map.getPlayerSpawnPoints().isEmpty(), "File map phải có điểm spawn cho Player ('P')");
        assertFalse(map.getEnemySpawnPoints().isEmpty(), "File map phải có điểm spawn cho Bot ('E')");


        for (Vector2D spawn : map.getPlayerSpawnPoints()) {
            assertEquals(0.0, spawn.x() % Environment.TILE_SIZE, "Tọa độ X của Player Spawn phải là bội số của TILE_SIZE");
            assertEquals(0.0, spawn.y() % Environment.TILE_SIZE, "Tọa độ Y của Player Spawn phải là bội số của TILE_SIZE");
        }


        for (Vector2D spawn : map.getEnemySpawnPoints()) {
            assertEquals(0.0, spawn.x() % Environment.TILE_SIZE, "Tọa độ X của Bot Spawn phải là bội số của TILE_SIZE");
            assertEquals(0.0, spawn.y() % Environment.TILE_SIZE, "Tọa độ Y của Bot Spawn phải là bội số của TILE_SIZE");
        }
    }

    @Test
    void testLoadMap_TileCoordinatesAndUniqueness() {
        GameMap gameMap = mapLoader.loadMap("/maps/stage1.map");
        List<Environment> map = gameMap.getEnvironments();

        for (Environment env : map) {

            assertEquals(0.0, env.getPosition().x() % Environment.TILE_SIZE);
            assertEquals(0.0, env.getPosition().y() % Environment.TILE_SIZE);
        }

        long countDistinct = map.stream().map(Environment::getPosition).distinct().count();
        assertEquals(map.size(), countDistinct);
    }

    @Test
    void testLoadMap_FileNotFound() {
        assertThrows(IllegalArgumentException.class, () -> mapLoader.loadMap("/maps/notFound.map"));
    }
}