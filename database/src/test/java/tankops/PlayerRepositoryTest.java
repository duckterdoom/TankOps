package tankops;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

public class PlayerRepositoryTest {

    @Test
    public void testCreateAndFindPlayer() {
        MongoDatabaseManager dbManager = new MongoDatabaseManager();
        PlayerRepository repo = new PlayerRepository(dbManager);

        String testUser = "TestTankerCloud";


        repo.createPlayer(testUser, "hash_123456");


        Optional<Document> found = repo.findByUsername(testUser);
        assertTrue(found.isPresent());
        assertEquals(testUser, found.get().getString("username"));

        dbManager.close();
    }
}