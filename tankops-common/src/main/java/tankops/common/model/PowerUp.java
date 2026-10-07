package tankops.common.model;

public class PowerUp extends Entity {
    private final PowerUpType type;
    private double lifetime; // Thời gian tồn tại trên màn hình (vd: 10-15s trước khi tự biến mất)

    public PowerUp(String id, Vector2D position, PowerUpType type) {
        super(id, position, 32.0, 32.0);
        this.type = type;
        this.lifetime = 12.0;
    }

    public PowerUpType getType() { return type; }
    public double getLifetime() { return lifetime; }
    public void updateLifetime(double deltaTime) { this.lifetime -= deltaTime; }
    public boolean isExpired() { return lifetime <= 0; }


}