package tankops.common.model;

import java.util.Objects;


public class PowerUp extends Entity {
    public static final int POINTS = 500; // điểm nhận khi nhặt

    private final PowerUpType type;

    public PowerUp(String id, Vector2D position, PowerUpType type) {
        super(id, position, Environment.TILE_SIZE, Environment.TILE_SIZE);
        this.type = Objects.requireNonNull(type, "type");
    }

    public PowerUpType getType() { return type; }
}
