package tankops;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerRepositoryTest {

    private MongoDatabaseManager dbManager;
    private PlayerRepository playerRepo;
    private final String testUser1 = "UnitTest_User01";
    private final String testUser2 = "UnitTest_User02";

    @BeforeEach
    public void setUp() {
        // 1. Khởi tạo kết nối DB trước mỗi bài test
        dbManager = new MongoDatabaseManager();
        playerRepo = new PlayerRepository(dbManager);

        // 2. Dọn dẹp dữ liệu cũ nếu còn sót lại từ lần test trước
        playerRepo.deletePlayer(testUser1);
        playerRepo.deletePlayer(testUser2);
    }

    @AfterEach
    public void tearDown() {
        // Xóa dữ liệu rác sau khi chạy xong bài test
        playerRepo.deletePlayer(testUser1);
        playerRepo.deletePlayer(testUser2);
        
        // Đóng kết nối DB
        dbManager.close();
    }

    @Test
    public void testRegisterPlayerSuccess() {
        // Đăng ký tài khoản mới thành công
        boolean isSuccess = playerRepo.registerPlayer(testUser1, "pass_hash_123");
        assertTrue(isSuccess, "Đăng ký tài khoản mới phải trả về true");

        // Kiểm tra dữ liệu đã lưu đúng chưa
        Document found = playerRepo.findByUsername(testUser1);
        assertNotNull(found, "Tài khoản vừa tạo phải tìm thấy trong DB");
        assertEquals(testUser1, found.getString("username"));
        assertEquals("pass_hash_123", found.getString("password_hash"));
        
        // Kiểm tra mảng match_ids xem đã khởi tạo chưa
        List<?> matchIds = found.getList("match_ids", ObjectId.class);
        assertNotNull(matchIds, "Mảng match_ids phải được khởi tạo");
        assertTrue(matchIds.isEmpty(), "Mảng match_ids ban đầu phải rỗng");
    }

    @Test
    public void testRegisterDuplicatePlayerFails() {
        // 1. Đăng ký tài khoản lần đầu
        playerRepo.registerPlayer(testUser1, "pass_hash_123");

        // 2. Đăng ký lại cùng username (Check trùng tên)
        boolean isSuccess = playerRepo.registerPlayer(testUser1, "pass_hash_diff");
        assertFalse(isSuccess, "Đăng ký trùng username phải trả về false và bị từ chối");
    }

    @Test
    public void testFindByNonExistingUser() {
        // Tìm tài khoản không tồn tại
        Document found = playerRepo.findByUsername("User_Khong_Ton_Tai_999");
        assertNull(found, "Tìm tài khoản không tồn tại phải trả về null");
    }

    @Test
    public void testAddMatchId() {
        // Đăng ký tài khoản
        playerRepo.registerPlayer(testUser1, "pass_hash_123");

        // Giả lập 1 ObjectId của trận đấu
        ObjectId mockMatchId = new ObjectId();
        playerRepo.addMatchId(testUser1, mockMatchId);

        // Kiểm tra xem ObjectId trận đấu đã được lưu vào mảng match_ids chưa
        Document found = playerRepo.findByUsername(testUser1);
        List<ObjectId> matchIds = found.getList("match_ids", ObjectId.class);
        
        assertNotNull(matchIds);
        assertEquals(1, matchIds.size(), "Mảng match_ids phải có đúng 1 phần tử");
        assertEquals(mockMatchId, matchIds.get(0), "Match ID lưu vào mảng phải khớp với ID khởi tạo");
    }

}