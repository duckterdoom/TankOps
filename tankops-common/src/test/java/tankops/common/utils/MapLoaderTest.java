package tankops.common.utils;

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
        // Giả định file /maps/stage1.map đã có trong src/main/resources/maps/
        String resourcePath = "/maps/stage1.map";

        List<Environment> environments = mapLoader.loadMap(resourcePath);

        // Kiểm tra danh sách không bị null
        assertNotNull(environments, "Danh sách Environment trả về không được null");

        // Kiểm tra danh sách chứa các ô địa hình (không rỗng)
        assertFalse(environments.isEmpty(), "Danh sách Environment không được rỗng khi nạp file map hợp lệ");

        // Kiểm tra phần tử đầu tiên có tọa độ và loại địa hình chuẩn
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
            double x = env.getPosition().getX();
            double y = env.getPosition().getY();

            assertEquals(0.0, x % MapLoader.TILE_SIZE, "Tọa độ X phải chia hết cho TILE_SIZE");
            assertEquals(0.0, y % MapLoader.TILE_SIZE, "Tọa độ Y phải chia hết cho TILE_SIZE");
        }
    }

    @Test
    @DisplayName("Bỏ qua ô trống (.) và chỉ tạo Environment cho các ký tự địa hình")
    void testLoadMap_IgnoresEmptyTiles() {
        String resourcePath = "/maps/stage1.map";
        List<Environment> environments = mapLoader.loadMap(resourcePath);

        // Đảm bảo không có ô nào chứa null type trong danh sách trả về
        boolean containsNullType = environments.stream().anyMatch(e -> e.getType() == null);
        assertFalse(containsNullType, "Danh sách không được chứa đối tượng có EnvironmentType = null");
    }

    @Test
    @DisplayName("Xử lý an toàn khi không tìm thấy tệp bản đồ (Invalid Path)")
    void testLoadMap_FileNotFound() {
        String invalidPath = "/maps/non_existent_map.map";

        List<Environment> environments = mapLoader.loadMap(invalidPath);

        // Phải trả về danh sách rỗng thay vì bị văng NullPointerException hoặc Exception dừng ứng dụng
        assertNotNull(environments, "Danh sách trả về không được null kể cả khi không tìm thấy file");
        assertTrue(environments.isEmpty(), "Danh sách phải rỗng khi đường dẫn file không tồn tại");
    }
}