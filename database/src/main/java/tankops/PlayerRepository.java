package tankops;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.Optional;

public class PlayerRepository {
    private final MongoCollection<Document> playerCollection;

    public PlayerRepository(MongoDatabaseManager dbManager) {
        // MongoDB sẽ tự động tạo collection "players" nếu chưa có khi ghi dữ liệu
        this.playerCollection = dbManager.getDatabase().getCollection("players");
    }

    // 1. Thêm người chơi mới (CREATE)
    public void createPlayer(String username, String passwordHash) {
        Document newPlayer = new Document("username", username)
                .append("password_hash", passwordHash)
                .append("total_score", 0)
                .append("matches_played", 0)
                .append("wins", 0);

        playerCollection.insertOne(newPlayer);
        System.out.println("Đã thêm người chơi lên MongoDB: " + username);
    }

    // 2. Tìm kiếm theo username (READ)
    public Optional<Document> findByUsername(String username) {
        Document player = playerCollection.find(Filters.eq("username", username)).first();
        return Optional.ofNullable(player);
    }

    // 3. Cập nhật điểm số sau trận đấu (UPDATE)
    public void updateScore(String username, int scoreToAdd, boolean isWinner) {
        int winIncrement = isWinner ? 1 : 0;
        playerCollection.updateOne(
                Filters.eq("username", username),
                Updates.combine(
                        Updates.inc("total_score", scoreToAdd),
                        Updates.inc("matches_played", 1),
                        Updates.inc("wins", winIncrement)
                )
        );
        System.out.println("Đã cập nhật chỉ số cho: " + username);
    }
}