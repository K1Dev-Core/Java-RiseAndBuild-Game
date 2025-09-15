package common;

import java.util.ArrayList;
import java.util.List;

public class MapLayer {
    private String name;
    private List<Tile> tiles;
    private boolean collider;

    public MapLayer(String name, boolean collider) {
        this.name = name;
        this.collider = collider;
        this.tiles = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<Tile> getTiles() {
        return tiles;
    }

    public boolean isCollider() {
        return collider;
    }

    public void addTile(Tile tile) {
        tiles.add(tile);
    }

    public Tile getTileAt(int x, int y) {
        for (Tile tile : tiles) {
            if (tile.getX() == x && tile.getY() == y) {
                return tile;
            }
        }
        return null;
    }

    public boolean hasCollisionAt(int x, int y) {
        if (!collider)
            return false;

        Tile tile = getTileAt(x, y);
        return tile != null && tile.isCollider();
    }
}
