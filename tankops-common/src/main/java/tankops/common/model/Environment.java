package tankops.common.model;

public class Environment extends Entity {
    private final EnvironmentType type;
    private int durability;

    public Environment(String id, Vector2D position, EnvironmentType type) {
        super(id, position, 32.0, 32.0); // Kích thước ô địa hình chuẩn 32x32px
        this.type = type;
        this.durability = type.getDefaultDurability();
    }

    public EnvironmentType getType() { return type; }
    public int getDurability() { return durability; }

    // Kiểm tra xe tăng có đi qua được ô này không
    public boolean canTankPass() {
        return type.isCanTankPass();
    }

    // Kiểm tra đạn có bay qua được ô này không
    public boolean canBulletPass() {
        return type.isCanBulletPass();
    }

    public void takeHit(int bulletStarLevel) {
        if (!type.isDestructible()) return; // Trees, Water, Ice không thể bị phá

        if (type == EnvironmentType.BRICK_WALL) {
            if (bulletStarLevel == 3) { // Tier 4 / Max power: Giảm sát thương nhanh gấp đôi nếu tier đạn là 4
                durability -= 2;
            } else {
                durability -= 1; // Bắn 4 phát thường thì hỏng
            }
        } else if (type == EnvironmentType.STEEL_WALL) {
            // Steel Wall CHỈ bị phá khi trúng đạn Tier 4 (starLevel == 3)
            if (bulletStarLevel == 3) {
                durability -= 1; // Cần 2 phát bắn Tier 4 để phá hoàn toàn
            }
        }

        if (durability < 0) durability = 0;
    }

    public boolean isDestroyed() {
        return type.isDestructible() && durability <= 0;
    }
}