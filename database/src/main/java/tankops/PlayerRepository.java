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

    // YÊU CẦU 1: Đăng ký (Có check trùng tên tài khoản)
    public boolean registerPlayer(String username, String passwordHash) {
        // Kiểm tra xem username đã tồn tại chưa
        Document existingPlayer = playerCollection.find(Filters.eq("username", username)).first();

        if (existingPlayer != null) {
            System.out.println("Lỗi: Tên tài khoản '" + username + "' đã bị trùng!");
            return false; // Trả về false, không cho tạo
        }

        // YÊU CẦU 2: Chỉ lưu name, pass và mảng chứa ID các trận đấu (match_ids)
        Document newPlayer = new Document("username", username)
                .append("password_hash", passwordHash)
                .append("match_ids", new ArrayList<ObjectId>()); // Mảng rỗng chờ thêm ID trận đấu

        playerCollection.insertOne(newPlayer);
        System.out.println("✅ Đăng ký thành công tài khoản: " + username);
        return true;
    }

    // Tìm kiếm người chơi
    public Document findByUsername(String username) {
        return playerCollection.find(Filters.eq("username", username)).first();
    }

    // Hàm mới: Thêm ID của trận đấu vào danh sách của người chơi (Sẽ gọi khi hết game)
    public void addMatchId(String username, ObjectId matchId) {
        playerCollection.updateOne(
                Filters.eq("username", username),
                Updates.push("match_ids", matchId) // Push (đẩy) ID trận đấu vào mảng
        );
    }

    // Xóa người chơi theo username (Dùng để dọn dẹp khi Test)
    public void deletePlayer(String username) {
        playerCollection.deleteOne(Filters.eq("username", username));
    }
}