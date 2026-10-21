package tankops.client.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tankops.common.model.common.Direction;
import tankops.common.net.game.TankSnapshot;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử InterpolationBuffer - Làm mịn chuyển động")
class InterpolationBufferTest {

    private TankSnapshot createTank(String id, double x, double y) {
        return new TankSnapshot(id, x, y, Direction.UP, 100, 100, 0, false, true, false);
    }

    @Test
    @DisplayName("Bộ đệm rỗng cập nhật không gây lỗi")
    void emptyBuffer_update_doesNotThrow() {
        InterpolationBuffer buffer = new InterpolationBuffer("tank-1");
        assertEquals(0, buffer.size());
        assertDoesNotThrow(() -> buffer.update(50));
        assertEquals(0.0, buffer.getInterpolatedX());
        assertEquals(0.0, buffer.getInterpolatedY());
        assertTrue(buffer.getLatestSnapshot().isEmpty());
    }

    @Test
    @DisplayName("Thêm 1 snapshot -> vị trí nội suy bằng chính snapshot đó")
    void singleSnapshot_returnsExactPosition() {
        InterpolationBuffer buffer = new InterpolationBuffer("tank-1");
        buffer.pushSnapshot(createTank("tank-1", 100.0, 200.0));

        assertEquals(1, buffer.size());
        buffer.update(0);

        assertEquals(100.0, buffer.getInterpolatedX(), 0.001);
        assertEquals(200.0, buffer.getInterpolatedY(), 0.001);
        assertTrue(buffer.getLatestSnapshot().isPresent());
        assertEquals(100.0, buffer.getLatestSnapshot().get().x());
    }

    @Test
    @DisplayName("Nội suy giữa hai snapshot (Lerp)")
    void twoSnapshots_interpolatesBetweenPositions() throws InterruptedException {
        InterpolationBuffer buffer = new InterpolationBuffer("tank-1");

        buffer.pushSnapshot(createTank("tank-1", 0.0, 0.0));
        Thread.sleep(60);
        buffer.pushSnapshot(createTank("tank-1", 100.0, 100.0));

        assertEquals(2, buffer.size());

        buffer.update(30);

        double x = buffer.getInterpolatedX();
        double y = buffer.getInterpolatedY();

        assertTrue(x >= 0.0 && x <= 100.0, "X phải nằm trong [0, 100], nhận được: " + x);
        assertTrue(y >= 0.0 && y <= 100.0, "Y phải nằm trong [0, 100], nhận được: " + y);
    }

    @Test
    @DisplayName("Xóa bộ đệm khi bắt đầu ván mới")
    void clearBuffer_resetsState() {
        InterpolationBuffer buffer = new InterpolationBuffer("tank-1");
        buffer.pushSnapshot(createTank("tank-1", 50.0, 50.0));
        assertEquals(1, buffer.size());

        buffer.clear();
        assertEquals(0, buffer.size());
        assertEquals(0.0, buffer.getInterpolatedX());
        assertEquals(0.0, buffer.getInterpolatedY());
    }
}
