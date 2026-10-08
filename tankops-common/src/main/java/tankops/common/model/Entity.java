package tankops.common.model;

import java.util.Objects;


public abstract class Entity {
    private final String id;
    private final double width;
    private final double height;
    private Vector2D position;

    protected Entity(String id, Vector2D position, double width, double height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width/height must be > 0");
        }
        this.id = Objects.requireNonNull(id, "id");
        this.position = Objects.requireNonNull(position, "position");
        this.width = width;
        this.height = height;
    }

    public String getId()         { return id; }
    public Vector2D getPosition() { return position; }
    public double getWidth()      { return width; }
    public double getHeight()     { return height; }

    public void setPosition(Vector2D position) {
        this.position = Objects.requireNonNull(position, "position");
    }


    public boolean intersects(Entity o) {
        return position.x() < o.position.x() + o.width && position.x() + width > o.position.x()
                && position.y() < o.position.y() + o.height && position.y() + height > o.position.y();
    }

    @Override
    public final boolean equals(Object o) {
        return this == o || (o instanceof Entity e && id.equals(e.id));
    }

    @Override
    public final int hashCode() { return id.hashCode(); }
}
