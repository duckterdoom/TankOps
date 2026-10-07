package tankops.common.utils;

import tankops.common.model.Environment;
import tankops.common.model.EnvironmentType;
import tankops.common.model.Vector2D;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MapLoader {
    // Kích thước chuẩn của từng tile ô vuông (32x32 pixel)
    public static final double TILE_SIZE = 32.0;


    public List<Environment> loadMap(String resourcePath) {
        List<Environment> environments = new ArrayList<>();

        // Nạp file từ thư mục resources
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("[MapLoader Error] Không tìm thấy tệp bản đồ tại: " + resourcePath);
                return environments;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            int row = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Bỏ qua dòng trống hoặc dòng chú thích (bắt đầu bằng dấu #)
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Tách từng ký tự/token phân cách bởi khoảng trắng
                String[] tokens = line.split("\\s+");

                for (int col = 0; col < tokens.length; col++) {
                    String token = tokens[col];

                    // Tính tọa độ pixel x, y trên bản đồ
                    double x = col * TILE_SIZE;
                    double y = row * TILE_SIZE;
                    Vector2D position = new Vector2D(x, y);

                    // Tạo ID duy nhất cho mỗi ô địa hình
                    String envId = "env_" + row + "_" + col;

                    EnvironmentType type = parseType(token);

                    // Nếu không phải ô trống thì tạo đối tượng Environment
                    if (type != null) {
                        environments.add(new Environment(envId, position, type));
                    }
                }
                row++; // Chuyển sang hàng tiếp theo
            }
        } catch (Exception e) {
            System.err.println("[MapLoader Error] Lỗi khi nạp file bản đồ: " + e.getMessage());
            e.printStackTrace();
        }

        return environments;
    }

    /**
     * Ánh xạ ký tự trong file .map thành enum EnvironmentType tương ứng.
     */
    private EnvironmentType parseType(String token) {
        return switch (token.toUpperCase()) {
            case "B" -> EnvironmentType.BRICK_WALL;
            case "S" -> EnvironmentType.STEEL_WALL;
            case "T" -> EnvironmentType.TREES;
            case "W" -> EnvironmentType.WATER;
            case "I" -> EnvironmentType.ICE;
            default  -> null; // Dấu "." hoặc ký tự khác đại diện cho ô trống (không vẽ Environment)
        };
    }
}