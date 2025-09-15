package common;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
// ใช้ SimpleJSON แทน org.json

public class GameMap {
    private int tileSize;
    private int mapWidth;
    private int mapHeight;
    private List<MapLayer> layers;
    private BufferedImage spritesheet;
    private Map<Integer, Image> tileSprites;

    public GameMap(String mapJsonPath, String spritesheetPath) {
        this.layers = new ArrayList<>();
        this.tileSprites = new HashMap<>();
        loadMap(mapJsonPath);
        loadSpritesheet(spritesheetPath);
        generateTileSprites();
    }

    private void loadMap(String mapJsonPath) {
        try {
            // อ่านไฟล์ JSON
            StringBuilder jsonContent = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(mapJsonPath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonContent.append(line);
                }
            }

            SimpleJSON.JSONObject mapData = SimpleJSON.parseObject(jsonContent.toString());

            // อ่านข้อมูลพื้นฐาน
            this.tileSize = mapData.getInt("tileSize");
            this.mapWidth = mapData.getInt("mapWidth");
            this.mapHeight = mapData.getInt("mapHeight");

            // อ่าน layers
            SimpleJSON.JSONArray layersArray = mapData.getJSONArray("layers");
            for (int i = 0; i < layersArray.length(); i++) {
                SimpleJSON.JSONObject layerData = layersArray.getJSONObject(i);
                String layerName = layerData.getString("name");
                boolean collider = layerData.getBoolean("collider");

                MapLayer layer = new MapLayer(layerName, collider);

                // อ่าน tiles ใน layer
                SimpleJSON.JSONArray tilesArray = layerData.getJSONArray("tiles");
                for (int j = 0; j < tilesArray.length(); j++) {
                    SimpleJSON.JSONObject tileData = tilesArray.getJSONObject(j);
                    int id = tileData.getInt("id");
                    int x = tileData.getInt("x");
                    int y = tileData.getInt("y");

                    Tile tile = new Tile(id, x, y, collider);
                    layer.addTile(tile);
                }

                this.layers.add(layer);
            }

            System.out.println(
                    "โหลดแผนที่สำเร็จ: " + mapWidth + "x" + mapHeight + " tiles, " + layers.size() + " layers");

        } catch (Exception e) {
            System.err.println("เกิดข้อผิดพลาดในการโหลดแผนที่: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadSpritesheet(String spritesheetPath) {
        try {
            this.spritesheet = ImageIO.read(new File(spritesheetPath));
            System.out.println("โหลด spritesheet สำเร็จ: " + spritesheet.getWidth() + "x" + spritesheet.getHeight());
        } catch (Exception e) {
            System.err.println("เกิดข้อผิดพลาดในการโหลด spritesheet: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generateTileSprites() {
        if (spritesheet == null)
            return;

        // คำนวณจำนวน tiles ใน spritesheet
        int tilesPerRow = spritesheet.getWidth() / tileSize;
        int tilesPerCol = spritesheet.getHeight() / tileSize;

        System.out.println("Spritesheet มี " + tilesPerRow + "x" + tilesPerCol + " tiles");

        // สร้าง sprites สำหรับแต่ละ tile ID
        for (MapLayer layer : layers) {
            for (Tile tile : layer.getTiles()) {
                int tileId = tile.getId();
                if (!tileSprites.containsKey(tileId)) {
                    // คำนวณตำแหน่งใน spritesheet
                    int spriteX = (tileId % tilesPerRow) * tileSize;
                    int spriteY = (tileId / tilesPerRow) * tileSize;

                    // ตัด sprite ออกมา
                    BufferedImage tileSprite = spritesheet.getSubimage(spriteX, spriteY, tileSize, tileSize);
                    tileSprites.put(tileId, tileSprite);
                    tile.setSprite(tileSprite);
                } else {
                    // ใช้ sprite ที่มีอยู่แล้ว
                    tile.setSprite(tileSprites.get(tileId));
                }
            }
        }

        System.out.println("สร้าง tile sprites สำเร็จ: " + tileSprites.size() + " tiles");
    }

    public void render(Graphics2D g2d, int cameraX, int cameraY, int screenWidth, int screenHeight) {
        // คำนวณขอบเขตที่ต้อง render
        int startX = Math.max(0, cameraX / tileSize);
        int endX = Math.min(mapWidth, (cameraX + screenWidth) / tileSize + 1);
        int startY = Math.max(0, cameraY / tileSize);
        int endY = Math.min(mapHeight, (cameraY + screenHeight) / tileSize + 1);

        // Render แต่ละ layer
        for (MapLayer layer : layers) {
            for (Tile tile : layer.getTiles()) {
                int tileX = tile.getX();
                int tileY = tile.getY();

                // ตรวจสอบว่า tile อยู่ในขอบเขตที่ต้อง render หรือไม่
                if (tileX >= startX && tileX < endX && tileY >= startY && tileY < endY) {
                    // คำนวณตำแหน่งบนหน้าจอ
                    int screenX = tileX * tileSize - cameraX;
                    int screenY = tileY * tileSize - cameraY;

                    // วาด tile
                    Image sprite = tile.getSprite();
                    if (sprite != null) {
                        g2d.drawImage(sprite, screenX, screenY, tileSize, tileSize, null);
                    }
                }
            }
        }
    }

    public boolean hasCollisionAt(int worldX, int worldY) {
        int tileX = worldX / tileSize;
        int tileY = worldY / tileSize;

        for (MapLayer layer : layers) {
            if (layer.hasCollisionAt(tileX, tileY)) {
                return true;
            }
        }
        return false;
    }

    public int getTileSize() {
        return tileSize;
    }

    public int getMapWidth() {
        return mapWidth;
    }

    public int getMapHeight() {
        return mapHeight;
    }

    public int getMapWidthPixels() {
        return mapWidth * tileSize;
    }

    public int getMapHeightPixels() {
        return mapHeight * tileSize;
    }

    public List<MapLayer> getLayers() {
        return layers;
    }
}
