package tankops.common.map;

import tankops.common.model.common.Vector2D;
import tankops.common.model.environment.*;

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

    public GameMap loadMap(String resourcePath) {
        try (InputStream is = MapLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Map not found: " + resourcePath);
            }
            return parse(new InputStreamReader(is, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read map " + resourcePath, e);
        }
    }

    public GameMap parse(Reader source) throws IOException {
        List<Environment> environments = new ArrayList<>();
        List<Vector2D> playerSpawnPoints = new ArrayList<>();
        List<Vector2D> enemySpawnPoints = new ArrayList<>();


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
                String token = tokens[col];
                if (EMPTY_TOKEN.equals(token)) continue;

                Vector2D position = new Vector2D(
                        col * Environment.TILE_SIZE,
                        row * Environment.TILE_SIZE
                );

                if(token.equals("P")){
                    playerSpawnPoints.add(position);
                    continue;
                }
                if(token.equals("E")){
                    enemySpawnPoints.add(position);
                    continue;
                }
                String id = "env_" + row + "_" + col;

                Environment env = createEnvironment(token, id, position, lineNumber);
                if (env != null) {
                    environments.add(env);
                }
            }
            row++;
        }
        return new GameMap(environments, playerSpawnPoints, enemySpawnPoints);
    }

    private Environment createEnvironment(String token, String id, Vector2D position, int lineNumber) {
        return switch (token) {
            case "B" -> new BrickWall(id, position);
            case "S" -> new SteelWall(id, position);
            case "T" -> new Trees(id, position);
            case "W" -> new Water(id, position);
            case "I" -> new Ice(id, position);
            default -> {
                System.out.println("Unknown title " + token + " at line " + lineNumber);
                yield null;
            }
        };
    }
}