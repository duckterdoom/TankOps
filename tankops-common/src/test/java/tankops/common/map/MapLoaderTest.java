package tankops.common.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        List<Environment> map = mapLoader.loadMap("/maps/stage1.map");

        assertNotNull(map);
        assertFalse(map.isEmpty());

        Environment first = map.get(0);
        assertNotNull(first.getId());
        assertNotNull(first.getPosition());
    }

    @Test
    void testLoadMap_TileCoordinatesAndUniqueness() {
        List<Environment> map = mapLoader.loadMap("/maps/stage1.map");

        for (Environment env : map) {
            // Tọa độ phải là bội số của TILE_SIZE (32.0)
            assertEquals(0.0, env.getPosition().x() % Environment.TILE_SIZE);
            assertEquals(0.0, env.getPosition().y() % Environment.TILE_SIZE);
        }

        // Đảm bảo không trùng vị trí
        long countDistinct = map.stream().map(Environment::getPosition).distinct().count();
        assertEquals(map.size(), countDistinct);
    }

    @Test
    void testLoadMap_FileNotFound() {
        assertThrows(IllegalArgumentException.class, () -> mapLoader.loadMap("/maps/notFound.map"));
    }
}