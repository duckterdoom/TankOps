
package tankops;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MatchRepositoryTest {

    @Test
    public void testSaveSinglePlayerResult() {

        // 1. Kết nối MongoDB
        MongoDatabaseManager dbManager = new MongoDatabaseManager();
        PlayerRepository playerRepo = new PlayerRepository(dbManager);
        MatchRepository matchRepo = new MatchRepository(dbManager, playerRepo);

        String username = "TestPlayer";

        try {
            // 2. Xóa tài khoản test cũ và tạo tài khoản mới
            playerRepo.deletePlayer(username);
            playerRepo.registerPlayer(username, "123456");

            // 3. Lưu kết quả trận đấu
            matchRepo.saveSinglePlayerResult(
                username, 1500, 5, 1, true
            );

            // 4. Lấy thông tin người chơi từ MongoDB
            Document player = playerRepo.findByUsername(username);
            assertNotNull(player);

            // 5. Lấy ID trận đấu mà người chơi đã lưu
            List<ObjectId> matchIds =
                player.getList("match_ids", ObjectId.class);

            assertEquals(1, matchIds.size());

            // 6. Tìm trận đấu bằng ID
            Document match = dbManager.getDatabase()
                .getCollection("matches")
                .find(new Document("_id", matchIds.get(0)))
                .first();

            // 7. Kiểm tra kết quả
            assertNotNull(match);
            assertEquals("SINGLE_PLAYER", match.getString("mode"));
            assertEquals(username, match.getString("winner_id"));

            System.out.println("Test thành công!");

        } finally {
            // 8. Xóa dữ liệu test và đóng kết nối
            playerRepo.deletePlayer(username);

            // Xóa thêm bản ghi trận đấu rác vừa tạo (nếu có)
            dbManager.getDatabase().getCollection("matches")
                    .deleteMany(new Document("winner_id", username));

            dbManager.close();
        }
    }
}
