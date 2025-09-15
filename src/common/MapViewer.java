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


// =====================
// TileAtlas: เก็บ mapping id → crop จาก tilesheet
// =====================
class TileAtlas {
    Map<Integer, Rectangle> atlas = new HashMap<>();
    int tileWidth, tileHeight;

    public static TileAtlas load(String path) throws IOException {
        TileAtlas atlas = new TileAtlas();
        List<String> lines = Files.readAllLines(Path.of(path));

        int currentId = -1, x = 0, y = 0;
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("tile_width:")) {
                atlas.tileWidth = Integer.parseInt(line.replaceAll("[^0-9]", ""));
            } else if (line.startsWith("tile_height:")) {
                atlas.tileHeight = Integer.parseInt(line.replaceAll("[^0-9]", ""));
            } else if (line.startsWith("id:")) {
                currentId = Integer.parseInt(line.replaceAll("[^0-9-]", ""));
            } else if (line.startsWith("x:")) {
                x = Integer.parseInt(line.replaceAll("[^0-9-]", ""));
            } else if (line.startsWith("y:")) {
                y = Integer.parseInt(line.replaceAll("[^0-9-]", ""));
                if (currentId >= 0) {
                    atlas.atlas.put(currentId, new Rectangle(x, y, atlas.tileWidth, atlas.tileHeight));
                    currentId = -1;
                }
            }
        }
        return atlas;
    }
}

// =====================
// LevelMap: โหลด tilemap
// =====================
class LevelMap {
    int width, height;
    int[][] tiles;

    public static LevelMap load(String path) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(path));
        LevelMap map = new LevelMap();
        List<Integer> data = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("width:")) {
                map.width = Integer.parseInt(line.replaceAll("[^0-9]", ""));
            } else if (line.startsWith("height:")) {
                map.height = Integer.parseInt(line.replaceAll("[^0-9]", ""));
            } else if (line.startsWith("data:")) {
                String nums = line.replace("data:", "").trim();
                String[] parts = nums.split(",");
                for (String p : parts) {
                    if (!p.isBlank()) {
                        data.add(Integer.parseInt(p.trim()));
                    }
                }
            }
        }

        map.tiles = new int[map.height][map.width];
        for (int y = 0; y < map.height; y++) {
            for (int x = 0; x < map.width; x++) {
                map.tiles[y][x] = data.get(y * map.width + x);
            }
        }
        return map;
    }
}

// =====================
// MapPanel: รองรับซูม/เลื่อนกล้อง
// =====================
class MapPanel extends JPanel implements KeyListener, MouseWheelListener, MouseMotionListener {
    BufferedImage sheet;
    TileAtlas atlas;
    LevelMap level;

    private double cameraX = 0, cameraY = 0;
    private double zoom = 1.0;

    private int lastMouseX, lastMouseY;
    private boolean dragging = false;

    public MapPanel(BufferedImage sheet, TileAtlas atlas, LevelMap level) {
        this.sheet = sheet;
        this.atlas = atlas;
        this.level = level;

        setPreferredSize(new Dimension(800, 600));

        addKeyListener(this);
        addMouseWheelListener(this);
        addMouseMotionListener(this);
        setFocusable(true);
        requestFocusInWindow();

        // mouse pressed/released สำหรับ drag
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragging = true;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragging = false;
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        AffineTransform old = g2d.getTransform();

        // Apply zoom และ camera
        g2d.translate(getWidth() / 2.0, getHeight() / 2.0);
        g2d.scale(zoom, zoom);
        g2d.translate(-cameraX, -cameraY);

        // วาด map
        for (int y = 0; y < level.height; y++) {
            for (int x = 0; x < level.width; x++) {
                int id = level.tiles[y][x];
                if (id < 0) continue;
                Rectangle r = atlas.atlas.get(id);
                if (r == null) continue;

                g2d.drawImage(sheet,
                        x * atlas.tileWidth, y * atlas.tileHeight,
                        x * atlas.tileWidth + atlas.tileWidth, y * atlas.tileHeight + atlas.tileHeight,
                        r.x, r.y, r.x + atlas.tileWidth, r.y + atlas.tileHeight,
                        null);
            }
        }

        g2d.setTransform(old);
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

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

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
    public void mouseMoved(MouseEvent e) {}
}

// =====================
// Main
// =====================
public class MapViewer {
    public static void main(String[] args) throws Exception {
        BufferedImage sheet = ImageIO.read(new File("./assets/map/tilesheet.png"));
        TileAtlas atlas = TileAtlas.load("./assets/map/tiles.tilesource");
        LevelMap level = LevelMap.load("./assets/map/level.tilemap");

        JFrame f = new JFrame("Tilemap Viewer");
        MapPanel panel = new MapPanel(sheet, atlas, level);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(panel);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}
