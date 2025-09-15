package common;

import java.awt.Image;

public class Tile {
    private int id;
    private int x;
    private int y;
    private boolean collider;
    private Image sprite;

    public Tile(int id, int x, int y, boolean collider) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.collider = collider;
    }

    public int getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isCollider() {
        return collider;
    }

    public Image getSprite() {
        return sprite;
    }

    public void setSprite(Image sprite) {
        this.sprite = sprite;
    }

    @Override
    public String toString() {
        return "Tile{id=" + id + ", x=" + x + ", y=" + y + ", collider=" + collider + "}";
    }
}
