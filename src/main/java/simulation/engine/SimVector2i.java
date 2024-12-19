package simulation.engine;

public class SimVector2i {
    public Integer x;
    public Integer y;

    public static final SimVector2i UP = new SimVector2i(0, 1);
    public static final SimVector2i DOWN = new SimVector2i(0, -1);
    public static final SimVector2i LEFT = new SimVector2i(-1, 0);
    public static final SimVector2i RIGHT = new SimVector2i(1, 0);

    public SimVector2i(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }

    public double distanceTo(SimVector2i other) {
        double dx = this.x - other.x;
        double dy = this.y - other.x;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public void add(SimVector2i vector) {
        this.x += vector.x;
        this.y += vector.y;
    }
    public void add(Integer x, Integer y) {
        this.x += x;
        this.y += y;
    }

    @Override
    public String toString() {
        return "Vector2i{" + "x=" + x + ", y=" + y + '}';
    }
}