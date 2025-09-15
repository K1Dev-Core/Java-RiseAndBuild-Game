import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;


// ===== Tile Struct =====
class Tile {
    int id, x, y;

    Tile(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }
}

class MapLayer {
    String name;
    List<Tile> tiles = new ArrayList<>();
}

class GameMap {
    int tileSize, mapWidth, mapHeight;
    List<MapLayer> layers = new ArrayList<>();
}

// ===== SpriteSheet Loader =====
class SpriteSheet {
    private BufferedImage sheet;
    private int tileSize;
    private int columns;

    public SpriteSheet(String path, int tileSize) throws IOException {
        this.sheet = ImageIO.read(new File(path));
        this.tileSize = tileSize;
        this.columns = sheet.getWidth() / tileSize;
    }

    public BufferedImage getTile(int id) {
        int x = (id % columns) * tileSize;
        int y = (id / columns) * tileSize;
        return sheet.getSubimage(x, y, tileSize, tileSize);
    }
}

// ===== Minimal JSON Parser =====
class SimpleJson {
    static GameMap loadMap(String path) throws IOException {
        String json = new String(Files.readAllBytes(Path.of(path)), java.nio.charset.StandardCharsets.UTF_8);
        GameMap map = new GameMap();

        map.tileSize = getInt(json, "\"tileSize\"");
        map.mapWidth = getInt(json, "\"mapWidth\"");
        map.mapHeight = getInt(json, "\"mapHeight\"");

        String[] layerParts = json.split("\\{\\s*\"name\"");
        for (int i = 1; i < layerParts.length; i++) {
            String part = layerParts[i];
            MapLayer layer = new MapLayer();

            layer.name = getString(part, "");
            String[] tileParts = part.split("\\{\\s*\"id\"");
            for (int j = 1; j < tileParts.length; j++) {
                String t = tileParts[j];
                int id = getInt(t, "");
                int x = getInt(t, "\"x\"");
                int y = getInt(t, "\"y\"");
                layer.tiles.add(new Tile(id, x, y));
            }
            map.layers.add(layer);
        }
        return map;
    }

    private static int getInt(String src, String key) {
        int idx = key.isEmpty() ? 0 : src.indexOf(key);
        if (idx == -1)
            return 0;
        String cut = key.isEmpty() ? src : src.substring(idx + key.length());
        cut = cut.replaceAll("[^0-9-]", " ").trim().split(" ")[0];
        return Integer.parseInt(cut);
    }

    private static String getString(String src, String key) {
        int idx = key.isEmpty() ? 0 : src.indexOf(key);
        if (idx == -1)
            return "";
        String cut = key.isEmpty() ? src : src.substring(idx + key.length());
        int q1 = cut.indexOf('"');
        int q2 = cut.indexOf('"', q1 + 1);
        return cut.substring(q1 + 1, q2);
    }
}

// ===== Panel แสดง Map + กล้อง + ซูม =====
class MapPanel extends JPanel implements KeyListener, MouseWheelListener, MouseMotionListener {
    private GameMap gameMap;
    private SpriteSheet spriteSheet;
    private double cameraX = 0, cameraY = 0;
    private double zoom = 1.0;

    private int lastMouseX, lastMouseY;
    private boolean dragging = false;

    public MapPanel(GameMap gameMap, SpriteSheet spriteSheet) {
        this.gameMap = gameMap;
        this.spriteSheet = spriteSheet;

        setPreferredSize(new Dimension(800, 600));

        addKeyListener(this);
        addMouseWheelListener(this);
        addMouseMotionListener(this);
        setFocusable(true);
        requestFocusInWindow();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        AffineTransform at = new AffineTransform();
        at.translate(-cameraX, -cameraY);
        at.scale(zoom, zoom);
        g2d.transform(at);

        for (MapLayer layer : gameMap.layers) {
            for (Tile t : layer.tiles) {
                BufferedImage tileImg = spriteSheet.getTile(t.id);
                g2d.drawImage(tileImg,
                        t.x * gameMap.tileSize,
                        t.y * gameMap.tileSize,
                        null);
            }
        }
    }

    // ===== Keyboard: เลื่อนกล้อง =====
    @Override
    public void keyPressed(KeyEvent e) {
        int step = (int) (20 / zoom);
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT -> cameraX -= step;
            case KeyEvent.VK_RIGHT -> cameraX += step;
            case KeyEvent.VK_UP -> cameraY -= step;
            case KeyEvent.VK_DOWN -> cameraY += step;
        }
        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    // ===== Mouse Scroll: Zoom =====
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double delta = 0.1;
        if (e.getPreciseWheelRotation() < 0) {
            zoom = Math.min(zoom + delta, 4.0);
        } else {
            zoom = Math.max(zoom - delta, 0.25);
        }
        repaint();
    }

    // ===== Mouse Drag: Pan =====
    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragging) {
            int dx = e.getX() - lastMouseX;
            int dy = e.getY() - lastMouseY;
            cameraX -= dx / zoom;
            cameraY -= dy / zoom;
            repaint();
        }
        lastMouseX = e.getX();
        lastMouseY = e.getY();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        lastMouseX = e.getX();
        lastMouseY = e.getY();
    }
}

// ===== Main =====
public class MapViewer {
    public static void main(String[] args) throws Exception {
        GameMap gameMap = SimpleJson.loadMap("./assets/map/map.json");
        SpriteSheet sheet = new SpriteSheet("./assets/map/spritesheet.png", gameMap.tileSize);

        JFrame frame = new JFrame("Game Map Viewer");
        MapPanel panel = new MapPanel(gameMap, sheet);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
