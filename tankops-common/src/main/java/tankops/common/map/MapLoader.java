package tankops.common.map;

import tankops.common.model.Environment;
import tankops.common.model.EnvironmentType;
import tankops.common.model.Vector2D;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class MapLoader {
    private static final String EMPTY_TOKEN = ".";


    public List<Environment> loadMap(String resourcePath) {
        try (InputStream is = MapLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Map not found: " + resourcePath);
            }
            return parse(new InputStreamReader(is, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read map " + resourcePath, e);
        }
    }

    public List<Environment> parse(Reader source) throws IOException {
        List<Environment> environments = new ArrayList<>();
        BufferedReader reader = new BufferedReader(source);

        String line;
        int lineNumber = 0;
        int row = 0;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] tokens = line.split("\\s+");
            for (int col = 0; col < tokens.length; col++) {
                Optional<EnvironmentType> type = parseType(tokens[col], lineNumber);
                if (type.isEmpty()) continue;

                Vector2D position = new Vector2D(
                        col * Environment.TILE_SIZE, row * Environment.TILE_SIZE);
                environments.add(new Environment("env_" + row + "_" + col, position, type.get()));
            }
            row++;
        }
        return environments;
    }

    private Optional<EnvironmentType> parseType(String token, int lineNumber) {
        return switch (token.toUpperCase()) {
            case "B" -> Optional.of(EnvironmentType.BRICK_WALL);
            case "S" -> Optional.of(EnvironmentType.STEEL_WALL);
            case "T" -> Optional.of(EnvironmentType.TREES);
            case "W" -> Optional.of(EnvironmentType.WATER);
            case "I" -> Optional.of(EnvironmentType.ICE);
            case EMPTY_TOKEN -> Optional.empty();
            default -> throw new IllegalArgumentException(
                    "Unknown tile '" + token + "' at line " + lineNumber);
        };
    }
}