package tankops.common.model;


public abstract class Entity {
    protected String id;
    protected Vector2D position;
    protected double width;
    protected double height;

    public Entity(String id, Vector2D position, double width, double height) {
        this.id = id;
        this.position = position;
        this.width = width;
        this.height = height;
    }

    public String getId() { return id; }
    public Vector2D getPosition() { return position; }
    public void setPosition(Vector2D position) { this.position = position; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }



}