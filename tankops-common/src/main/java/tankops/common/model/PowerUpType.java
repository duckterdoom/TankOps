package tankops.common.model;

public enum PowerUpType {
    GRENADE, // Phá hủy toàn bộ enemy trên màn hình
    HELMET,  // Tạo giáp/bảo vệ tạm thời
    SHOVEL,  // Biến tường gạch quanh căn cứ thành tường thép tạm thời
    STAR,    // Tăng 1 tier công phá cho đạn
    TANK,    // Cộng thêm 1 mạng (extra life)
    TIMER    // Đóng băng toàn bộ enemy
}