package common;

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
// CollisionGroup: เก็บข้อมูล collision group
// =====================
class CollisionGroup {
    int index;
    int count;
    String groupName;

    public CollisionGroup(int index, int count, String groupName) {
        this.index = index;
        this.count = count;
        this.groupName = groupName;
    }
}

// =====================
// TileAtlas: เก็บ mapping id → crop จาก tilesheet
// =====================
class TileAtlas {
    Map<Integer, Rectangle> atlas = new HashMap<>();
    Map<Integer, String> collisionGroups = new HashMap<>();
    int tileWidth, tileHeight;

    public static TileAtlas load(String path) throws IOException {
        TileAtlas atlas = new TileAtlas();
        List<String> lines = Files.readAllLines(Path.of(path));

        // อ่านข้อมูลพื้นฐาน
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("tile_width:")) {
                atlas.tileWidth = Integer.parseInt(line.substring(11).trim());
            } else if (line.startsWith("tile_height:")) {
                atlas.tileHeight = Integer.parseInt(line.substring(12).trim());
            }
        }

        // อ่าน collision groups
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.startsWith("convex_hulls {")) {
                int index = -1;
                String groupName = "";

                // อ่านข้อมูลใน convex_hulls block
                for (int j = i + 1; j < lines.size(); j++) {
                    String hullLine = lines.get(j).trim();
                    if (hullLine.startsWith("index:")) {
                        index = Integer.parseInt(hullLine.substring(6).trim());
                    } else if (hullLine.startsWith("collision_group:")) {
                        groupName = hullLine.substring(16).replaceAll("\"", "").trim();
                    } else if (hullLine.startsWith("}")) {
                        break;
                    }
                }

                if (index >= 0 && !groupName.isEmpty()) {
                    atlas.collisionGroups.put(index, groupName);
                }
            }
        }

        // สร้าง atlas จาก tilesheet (grid-based)
        BufferedImage sheet = ImageIO.read(new File("./assets/map/tilesheet.png"));
        int tilesPerRow = sheet.getWidth() / atlas.tileWidth;
        int tilesPerCol = sheet.getHeight() / atlas.tileHeight;

        System.out.println("Tilesheet size: " + sheet.getWidth() + "x" + sheet.getHeight());
        System.out.println("Tile size: " + atlas.tileWidth + "x" + atlas.tileHeight);
        System.out.println("Tiles per row: " + tilesPerRow + ", Tiles per col: " + tilesPerCol);
        System.out.println("Collision groups loaded: " + atlas.collisionGroups.size());

        for (int y = 0; y < tilesPerCol; y++) {
            for (int x = 0; x < tilesPerRow; x++) {
                int tileId = y * tilesPerRow + x;
                int pixelX = x * atlas.tileWidth;
                int pixelY = y * atlas.tileHeight;
                atlas.atlas.put(tileId, new Rectangle(pixelX, pixelY, atlas.tileWidth, atlas.tileHeight));
            }
        }

        return atlas;
    }
}

// =====================
// TileCell: เก็บข้อมูลแต่ละ cell
// =====================
class TileCell {
    int x, y, tile;
    boolean hFlip, vFlip;

    public TileCell(int x, int y, int tile, boolean hFlip, boolean vFlip) {
        this.x = x;
        this.y = y;
        this.tile = tile;
        this.hFlip = hFlip;
        this.vFlip = vFlip;
    }
}

// =====================
// MapLayer: เก็บข้อมูล layer
// =====================
class MapLayer {
    String id;
    double z;
    boolean isVisible;
    List<TileCell> cells;

    public MapLayer(String id, double z, boolean isVisible) {
        this.id = id;
        this.z = z;
        this.isVisible = isVisible;
        this.cells = new ArrayList<>();
    }
}

// =====================
// LevelMap: โหลด tilemap
// =====================
class LevelMap {
    int width, height;
    List<MapLayer> layers;

    public LevelMap() {
        this.layers = new ArrayList<>();
    }

    public static LevelMap load(String path) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(path));
        LevelMap map = new LevelMap();

        MapLayer currentLayer = null;
        int currentX = 0, currentY = 0, currentTile = 0;
        boolean currentHFlip = false, currentVFlip = false;
        boolean inCell = false;

        for (String line : lines) {
            line = line.trim();

            if (line.startsWith("layers {")) {
                // เริ่ม layer ใหม่
                currentLayer = null;
            } else if (line.startsWith("id: \"")) {
                String id = line.substring(5, line.length() - 1);
                currentLayer = new MapLayer(id, 0, true);
            } else if (line.startsWith("z:")) {
                if (currentLayer != null) {
                    currentLayer.z = Double.parseDouble(line.substring(2).trim());
                }
            } else if (line.startsWith("is_visible:")) {
                if (currentLayer != null) {
                    currentLayer.isVisible = line.contains("1");
                }
            } else if (line.startsWith("cell {")) {
                inCell = true;
                currentX = 0;
                currentY = 0;
                currentTile = 0;
                currentHFlip = false;
                currentVFlip = false;
            } else if (line.startsWith("x:")) {
                if (inCell) {
                    currentX = Integer.parseInt(line.substring(2).trim());
                }
            } else if (line.startsWith("y:")) {
                if (inCell) {
                    currentY = Integer.parseInt(line.substring(2).trim());
                }
            } else if (line.startsWith("tile:")) {
                if (inCell) {
                    currentTile = Integer.parseInt(line.substring(5).trim());
                }
            } else if (line.startsWith("h_flip:")) {
                if (inCell) {
                    currentHFlip = line.contains("1");
                }
            } else if (line.startsWith("v_flip:")) {
                if (inCell) {
                    currentVFlip = line.contains("1");
                }
            } else if (line.startsWith("}")) {
                if (inCell) {
                    // จบ cell
                    if (currentLayer != null) {
                        currentLayer.cells
                                .add(new TileCell(currentX, currentY, currentTile, currentHFlip, currentVFlip));
                    }
                    inCell = false;
                } else if (currentLayer != null) {
                    // จบ layer
                    map.layers.add(currentLayer);
                    currentLayer = null;
                }
            }
        }

        // คำนวณขนาดแผนที่
        map.width = 0;
        map.height = 0;
        for (MapLayer layer : map.layers) {
            for (TileCell cell : layer.cells) {
                map.width = Math.max(map.width, cell.x + 1);
                map.height = Math.max(map.height, cell.y + 1);
            }
        }

        // เรียงลำดับ layers ตาม z
        map.layers.sort((a, b) -> Double.compare(a.z, b.z));

        System.out.println("Map loaded:");
        System.out.println("  Size: " + map.width + "x" + map.height);
        System.out.println("  Layers: " + map.layers.size());
        for (MapLayer layer : map.layers) {
            System.out.println("    - " + layer.id + " (z:" + layer.z + ", visible:" + layer.isVisible + ", cells:"
                    + layer.cells.size() + ")");
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

    public double cameraX = 0, cameraY = 0;
    public double zoom = 1.0;
    private boolean showCollision = false;

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

        // วาด map ตาม layers
        for (MapLayer layer : level.layers) {
            if (!layer.isVisible)
                continue;

            for (TileCell cell : layer.cells) {
                if (cell.tile < 0)
                    continue;
                Rectangle r = atlas.atlas.get(cell.tile);
                if (r == null)
                    continue;

                int drawX = cell.x * atlas.tileWidth;
                int drawY = (level.height - 1 - cell.y) * atlas.tileHeight;
                int drawWidth = atlas.tileWidth;
                int drawHeight = atlas.tileHeight;

                // จัดการ flip
                if (cell.hFlip || cell.vFlip) {
                    AffineTransform cellTransform = new AffineTransform();
                    cellTransform.translate(drawX + atlas.tileWidth / 2.0, drawY + atlas.tileHeight / 2.0);
                    if (cell.hFlip)
                        cellTransform.scale(-1, 1);
                    if (cell.vFlip)
                        cellTransform.scale(1, -1);
                    cellTransform.translate(-atlas.tileWidth / 2.0, -atlas.tileHeight / 2.0);

                    AffineTransform oldTransform = g2d.getTransform();
                    g2d.transform(cellTransform);

                    g2d.drawImage(sheet,
                            0, 0, atlas.tileWidth, atlas.tileHeight,
                            r.x, r.y, r.x + atlas.tileWidth, r.y + atlas.tileHeight,
                            null);

                    g2d.setTransform(oldTransform);
                } else {
                    g2d.drawImage(sheet,
                            drawX, drawY, drawX + drawWidth, drawY + drawHeight,
                            r.x, r.y, r.x + atlas.tileWidth, r.y + atlas.tileHeight,
                            null);
                }
            }
        }

        // วาด collision areas (ถ้าเปิดใช้งาน)
        if (showCollision) {
            g2d.setColor(new Color(255, 0, 0, 100));
            for (MapLayer layer : level.layers) {
                if (!layer.isVisible)
                    continue;

                for (TileCell cell : layer.cells) {
                    if (cell.tile < 0)
                        continue;
                    String collisionGroup = atlas.collisionGroups.get(cell.tile);
                    if (collisionGroup != null) {
                        int drawX = cell.x * atlas.tileWidth;
                        int drawY = (level.height - 1 - cell.y) * atlas.tileHeight;
                        g2d.fillRect(drawX, drawY, atlas.tileWidth, atlas.tileHeight);
                    }
                }
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
            case KeyEvent.VK_C -> showCollision = !showCollision; // เปิด/ปิด collision display
            case KeyEvent.VK_R -> { // Reset camera
                cameraX = level.width * atlas.tileWidth / 2.0;
                cameraY = level.height * atlas.tileHeight / 2.0;
                zoom = 1.0;
            }
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
    }
}

// =====================
// Main
// =====================
public class MapViewer {
    public static void main(String[] args) throws Exception {
        BufferedImage sheet = ImageIO.read(new File("./assets/map/tilesheet.png"));
        TileAtlas atlas = TileAtlas.load("./assets/map/tiles.tilesource");
        LevelMap level = LevelMap.load("./assets/map/level.tilemap");

        JFrame f = new JFrame("Tilemap Viewer - กด C เพื่อแสดง collision, R เพื่อ reset กล้อง");
        MapPanel panel = new MapPanel(sheet, atlas, level);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(panel);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);

        // ตั้งค่ากล้องเริ่มต้นให้อยู่กลางแผนที่
        panel.cameraX = level.width * atlas.tileWidth / 2.0;
        panel.cameraY = level.height * atlas.tileHeight / 2.0;
        panel.repaint();
    }
}
