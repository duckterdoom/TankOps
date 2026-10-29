package tankops;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerRepositoryTest {

    @Test
    public void testRegisterAndCheckDuplicate() {
        MongoDatabaseManager dbManager = new MongoDatabaseManager();
        PlayerRepository repo = new PlayerRepository(dbManager);

        String testUser = "TestUserNoPass";

        try {
            // Xóa người chơi cũ nếu có
            repo.deletePlayer(testUser);

            // 1. Đăng ký thành công lần đầu
            boolean firstRegister = repo.registerPlayer(testUser);
            assertTrue(firstRegister, "Lần đầu đăng ký phải thành công");

            // 2. Kiểm tra tài khoản đã tồn tại trong DB chưa
            Document found = repo.findByUsername(testUser);
            assertNotNull(found);
            assertEquals(testUser, found.getString("username"));

            // 3. Đăng ký lại cùng tên -> Phải bị từ chối do trùng tên
            boolean duplicateRegister = repo.registerPlayer(testUser);
            assertFalse(duplicateRegister, "Đăng ký trùng tên phải bị từ chối");

        } finally {
            // Dọn dẹp dữ liệu test
            repo.deletePlayer(testUser);
            dbManager.close();
        }
    }
}