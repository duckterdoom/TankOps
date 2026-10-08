package tankops.common.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tankops.common.model.Environment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MapLoaderTest {

    private MapLoader mapLoader;

    @BeforeEach
    void setUp() {
        mapLoader = new MapLoader();
    }

    @Test
    @DisplayName("Nạp bản đồ hợp lệ thành công và đúng số lượng ô địa hình")
    void testLoadMap_Success() {

        String resourcePath = "/maps/stage1.map";

        List<Environment> environments = mapLoader.loadMap(resourcePath);

        assertNotNull(environments, "Danh sách Environment trả về không được null");
        assertFalse(environments.isEmpty(), "Danh sách Environment không được rỗng khi nạp file map hợp lệ");

        Environment firstTile = environments.get(0);
        assertNotNull(firstTile.getId(), "ID của Environment không được null");
        assertNotNull(firstTile.getPosition(), "Tọa độ của Environment không được null");
        assertNotNull(firstTile.getType(), "Loại EnvironmentType không được null");
    }

    @Test
    @DisplayName("Tọa độ pixel (X, Y) của các ô được tính toán chính xác theo TILE_SIZE")
    void testLoadMap_TileCoordinates() {
        String resourcePath = "/maps/stage1.map";
        List<Environment> environments = mapLoader.loadMap(resourcePath);

        // Mọi ô địa hình phải có tọa độ x, y là bội số của TILE_SIZE (32.0)
        for (Environment env : environments) {
            double x = env.getPosition().x();
            double y = env.getPosition().y();

            assertEquals(0.0, x % Environment.TILE_SIZE, "Tọa độ X phải chia hết cho TILE_SIZE");
            assertEquals(0.0, y % Environment.TILE_SIZE, "Tọa độ Y phải chia hết cho TILE_SIZE");
        }
    }

    @Test
    @DisplayName("Không có hai ô địa hình nào trùng vị trí")
    void testLoadMap_NoDuplicatePositions() {
        String resourcePath = "/maps/stage1.map";
        List<Environment> environments = mapLoader.loadMap(resourcePath);

        long uniquePositions = environments.stream()
                .map(Environment::getPosition)
                .distinct()
                .count();

        assertEquals(environments.size(), uniquePositions, "Mỗi ô địa hình phải có vị trí riêng");
    }

    @Test
    @DisplayName("Ném lỗi khi không tìm thấy tệp bản đồ (Invalid Path)")
    void testLoadMap_FileNotFound() {
        String invalidPath = "/maps/non_existent_map.map";

        assertThrows(IllegalArgumentException.class, () -> mapLoader.loadMap(invalidPath));
    }
}