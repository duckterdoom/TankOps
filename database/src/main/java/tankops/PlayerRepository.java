package tankops;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;

public class PlayerRepository {

    private final MongoCollection<Document> playerCollection;

    public PlayerRepository(MongoDatabaseManager dbManager) {
        playerCollection = dbManager.getDatabase().getCollection("players");
    }

    // 1. Tạo người chơi mới (Chỉ lưu username và danh sách match_ids)
    public void createPlayer(String username) {
        Document player = new Document("username", username)
                .append("match_ids", new ArrayList<ObjectId>());

        playerCollection.insertOne(player);
        System.out.println("Đã thêm người chơi: " + username);
    }

    // 2. Tìm người chơi theo username
    public Document findByUsername(String username) {
        return playerCollection.find(Filters.eq("username", username)).first();
    }

    // 3. Đăng ký tài khoản (Chỉ kiểm tra trùng tên username)
    public boolean registerPlayer(String username) {
        Document existingPlayer = findByUsername(username);

        if (existingPlayer != null) {
            System.out.println("❌ Tên tài khoản '" + username + "' đã bị trùng!");
            return false;
        }

        createPlayer(username);
        System.out.println("✅ Đăng ký tài khoản thành công: " + username);
        return true;
    }

    // 4. Thêm ID trận đấu vào danh sách match_ids của người chơi
    public void addMatchId(String username, ObjectId matchId) {
        playerCollection.updateOne(
                Filters.eq("username", username),
                Updates.push("match_ids", matchId)
        );
    }

    // 5. Xóa người chơi theo username (dùng dọn dẹp khi test)
    public void deletePlayer(String username) {
        playerCollection.deleteOne(Filters.eq("username", username));
        System.out.println("Đã xóa người chơi: " + username);
    }
}