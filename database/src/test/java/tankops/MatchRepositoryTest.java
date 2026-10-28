package tankops;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MatchRepositoryTest {

    @Test
    public void testSaveSinglePlayerResult() {

        MongoDatabaseManager dbManager = new MongoDatabaseManager();
        PlayerRepository playerRepo = new PlayerRepository(dbManager);
        MatchRepository matchRepo = new MatchRepository(dbManager, playerRepo);

        String username = "TestPlayerNoPass";

        try {
            // 1. Xóa tài khoản cũ và đăng ký tài khoản mới (không cần password)
            playerRepo.deletePlayer(username);
            playerRepo.registerPlayer(username);

            // 2. Lưu kết quả trận đấu
            matchRepo.saveSinglePlayerResult(username, 1500, 5, 1, true);

            // 3. Lấy thông tin người chơi từ MongoDB
            Document player = playerRepo.findByUsername(username);
            assertNotNull(player);

            // 4. Lấy ID trận đấu mà người chơi đã lưu
            List<ObjectId> matchIds = player.getList("match_ids", ObjectId.class);
            assertEquals(1, matchIds.size());

            // 5. Tìm trận đấu bằng ID vừa lưu
            Document match = dbManager.getDatabase()
                    .getCollection("matches")
                    .find(new Document("_id", matchIds.get(0)))
                    .first();

            // 6. Kiểm tra kết quả trận đấu
            assertNotNull(match);
            assertEquals("SINGLE_PLAYER", match.getString("mode"));
            assertEquals(username, match.getString("winner_id"));

            System.out.println("Test MatchRepository thành công!");

        } finally {
            // Dọn dẹp dữ liệu test
            playerRepo.deletePlayer(username);
            dbManager.getDatabase().getCollection("matches")
                    .deleteMany(new Document("winner_id", username));
            dbManager.close();
        }
    }
}