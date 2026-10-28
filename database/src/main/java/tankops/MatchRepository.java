package tankops; // Cập nhật package cho phù hợp với cấu trúc dự án của bạn

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;

import tankops.MongoDatabaseManager; // Giả sử MongoDatabaseManager nằm ở package tankops
import tankops.PlayerRepository; // Giả sử PlayerRepository nằm ở package tankops

import java.util.Date;
import java.util.List;

public class MatchRepository {

    // Bộ sưu tập (collection) 'matches' trong MongoDB
    private final MongoCollection<Document> matchCollection;

    // Cần PlayerRepository để cập nhật ID trận đấu vào tài khoản người chơi
    private final PlayerRepository playerRepository;

    // Constructor: Khởi tạo kết nối tới bảng matches
    public MatchRepository(MongoDatabaseManager dbManager, PlayerRepository playerRepository) {
        this.matchCollection = dbManager.getDatabase().getCollection("matches");
        this.playerRepository = playerRepository;
    }

    /**
     * Hàm này được gọi khi một trận đấu kết thúc.
     * Nó sẽ lưu toàn bộ thông tin trận đấu, bao gồm kết quả của từng người chơi,
     * để sau này ResultPanel có thể lấy ra và hiển thị.
     *
     * @param roomName Tên phòng chơi
     * @param playerResults Danh sách các Document, mỗi Document chứa: username, kills, deaths, score, is_winner
     */
    public void saveMatch(String roomName, List<Document> playerResults) {

        // 1. Gói gọn tất cả thông tin vào một Document (tương đương 1 dòng trong SQL)
        Document matchDocument = new Document("room_name", roomName)
                .append("ended_at", new Date()) // Lưu thời gian kết thúc trận
                .append("player_results", playerResults); // Nhúng trực tiếp mảng kết quả vào đây

        // 2. Lưu Document này vào MongoDB
        matchCollection.insertOne(matchDocument);

        // 3. Lấy ra ID (mã định danh) mà MongoDB vừa tự động tạo cho trận đấu này
        ObjectId matchId = matchDocument.getObjectId("_id");

        // 4. Lặp qua từng người chơi trong trận, thêm ID trận đấu này vào hồ sơ của họ
        for (Document result : playerResults) {
            String username = result.getString("username");

            // Gọi hàm addMatchId (đã được định nghĩa trong PlayerRepository)
            playerRepository.addMatchId(username, matchId);
        }

        System.out.println("✅ Đã lưu kết quả trận đấu! Match ID: " + matchId);
    }

    public void saveSinglePlayerResult(
            String username,
            int score,
            int kills,
            int deaths,
            boolean won) {

        Document player = new Document("username", username)
                .append("score", score)
                .append("kills", kills)
                .append("deaths", deaths)
                .append("status", won ? "VICTORIOUS" : "DEFEATED");

        List<Document> players = List.of(player);

        Document match = new Document("mode", "SINGLE_PLAYER")
                .append("winner_id", won ? username : null)
                .append("players", players);

        matchCollection.insertOne(match);

        ObjectId matchId = match.getObjectId("_id");

        playerRepository.addMatchId(username, matchId);
    }

}