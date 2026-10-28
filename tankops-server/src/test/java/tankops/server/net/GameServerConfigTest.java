package tankops.server.net;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class GameServerConfigTest {
    @TempDir Path directory;

    @Test
    void deploymentFileOverridesBundledDefaultsAndKeepsFiveSpawns() throws Exception {
        Path config = directory.resolve("config.properties");
        Files.writeString(config, "server.maxRooms=3\nserver.defaultRooms=1\nroom.maxPlayers=5\nserver.maxClients=16\n");
        String previous = System.getProperty("tankops.config");
        try {
            System.setProperty("tankops.config", config.toString());
            GameServer server = new GameServer();
            assertEquals(1, server.getRoomManager().listRooms().size());
            assertEquals(5, server.getRoomManager().listRooms().get(0).getMaxPlayers());
            assertEquals(16, server.getMaxClients());
            assertEquals(5, server.loadMultiMap().getPlayerSpawnPoints().size());
        } finally {
            if (previous == null) System.clearProperty("tankops.config");
            else System.setProperty("tankops.config", previous);
        }
    }
}
