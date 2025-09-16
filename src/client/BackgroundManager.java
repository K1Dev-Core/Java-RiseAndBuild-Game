package client;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class BackgroundManager {
    private final int[][] terrainNoise;
    private final int noiseSize = 256;
    private final float noiseScale = 0.1f;
    private final int[] cloudPositions;
    private final int[] treePositions;
    private final int[] rockPositions;
    
    private final Map<String, BufferedImage> terrainCache = new HashMap<>();
    private final int TILE_SIZE = 256;
    private final int CACHE_RANGE = 5;
    
    public BackgroundManager() {
        this.terrainNoise = new int[noiseSize][noiseSize];
        this.cloudPositions = new int[20];
        this.treePositions = new int[50];
        this.rockPositions = new int[30];
        generateTerrainNoise();
        generateBackgroundObjects();
        preRenderTerrain();
    }
    
    private void generateTerrainNoise() {
        for (int x = 0; x < noiseSize; x++) {
            for (int y = 0; y < noiseSize; y++) {
                float noise1 = (float) Math.sin(x * noiseScale) * (float) Math.cos(y * noiseScale);
                float noise2 = (float) Math.sin(x * noiseScale * 2) * (float) Math.cos(y * noiseScale * 2) * 0.5f;
                float noise3 = (float) Math.sin(x * noiseScale * 0.5f) * (float) Math.cos(y * noiseScale * 0.5f) * 0.3f;
                float combinedNoise = noise1 + noise2 + noise3;
                this.terrainNoise[x][y] = (int) ((combinedNoise + 1) * 50);
            }
        }
    }
    
    private void generateBackgroundObjects() {
        int mapWidth = 2000;
        for (int i = 0; i < cloudPositions.length; i++) {
            cloudPositions[i] = (int) (Math.random() * mapWidth);
        }
        for (int i = 0; i < treePositions.length; i++) {
            treePositions[i] = (int) (Math.random() * mapWidth);
        }
        for (int i = 0; i < rockPositions.length; i++) {
            rockPositions[i] = (int) (Math.random() * mapWidth);
        }
    }
    
    private int getPlayerX(Object player) {
        try {
            return (Integer) player.getClass().getField("x").get(player);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private int getPlayerY(Object player) {
        try {
            return (Integer) player.getClass().getField("y").get(player);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private void preRenderTerrain() {
        for (int tileX = -CACHE_RANGE; tileX <= CACHE_RANGE; tileX++) {
            for (int tileY = -CACHE_RANGE; tileY <= CACHE_RANGE; tileY++) {
                String key = tileX + "," + tileY;
                if (!terrainCache.containsKey(key)) {
                    BufferedImage tile = createTerrainTile(tileX, tileY);
                    terrainCache.put(key, tile);
                }
            }
        }
    }
    
    private BufferedImage createTerrainTile(int tileX, int tileY) {
        BufferedImage tile = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = tile.createGraphics();
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        
        int startX = tileX * TILE_SIZE;
        int startY = tileY * TILE_SIZE;
        
        for (int x = 0; x < TILE_SIZE; x += 64) {
            for (int y = 0; y < TILE_SIZE; y += 64) {
                int worldX = startX + x;
                int worldY = startY + y;
                
                int noiseX = Math.abs(worldX) % noiseSize;
                int noiseY = Math.abs(worldY) % noiseSize;
                int noiseValue = terrainNoise[noiseX][noiseY];
                
                Color terrainColor = getTerrainColor(noiseValue);
                g2d.setColor(terrainColor);
                g2d.fillRect(x, y, 64, 64);
            }
        }
        
        g2d.dispose();
        return tile;
    }
    
    public void drawBackground(Graphics2D g2d, Object mainPlayer, int screenWidth, int screenHeight, float zoom) {
        if (mainPlayer == null) return;
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        
        drawSimpleBackground(g2d, screenWidth, screenHeight);
        drawSimpleGrid(g2d, mainPlayer, screenWidth, screenHeight, zoom);
        drawMapBounds(g2d, mainPlayer, screenWidth, screenHeight, zoom);
    }
    
    private void drawCachedTerrain(Graphics2D g2d, Object mainPlayer, int screenWidth, int screenHeight, float zoom) {
        int playerScreenX = screenWidth / 2;
        int playerScreenY = screenHeight / 2;
        
        int tileSize = (int) (TILE_SIZE * zoom);
        int playerX = getPlayerX(mainPlayer);
        int playerY = getPlayerY(mainPlayer);
        int playerTileX = (int) (playerX / TILE_SIZE);
        int playerTileY = (int) (playerY / TILE_SIZE);
        
        int bufferZone = 2;
        
        for (int tileX = playerTileX - bufferZone; tileX <= playerTileX + bufferZone; tileX++) {
            for (int tileY = playerTileY - bufferZone; tileY <= playerTileY + bufferZone; tileY++) {
                String key = tileX + "," + tileY;
                
                if (!terrainCache.containsKey(key)) {
                    BufferedImage tile = createTerrainTile(tileX, tileY);
                    terrainCache.put(key, tile);
                }
                
                BufferedImage tile = terrainCache.get(key);
                int screenX = playerScreenX - (int) ((playerX - tileX * TILE_SIZE) * zoom);
                int screenY = playerScreenY - (int) ((playerY - tileY * TILE_SIZE) * zoom);
                
                if (screenX > -tileSize && screenX < screenWidth + tileSize && 
                    screenY > -tileSize && screenY < screenHeight + tileSize) {
                    g2d.drawImage(tile, screenX, screenY, tileSize, tileSize, null);
                }
            }
        }
    }
    
    private void drawSimpleBackground(Graphics2D g2d, int screenWidth, int screenHeight) {
        g2d.setColor(new Color(34, 139, 34));
        g2d.fillRect(0, 0, screenWidth, screenHeight);
    }
    
    private void drawSimpleGrid(Graphics2D g2d, Object mainPlayer, int screenWidth, int screenHeight, float zoom) {
        int playerX = getPlayerX(mainPlayer);
        int playerY = getPlayerY(mainPlayer);
        
        int tileSize = (int) (32 * zoom);
        int playerScreenX = screenWidth / 2;
        int playerScreenY = screenHeight / 2;
        
        int offsetX = (int) ((playerX % 32) * zoom);
        int offsetY = (int) ((playerY % 32) * zoom);
        
        int startX = playerScreenX - offsetX;
        int startY = playerScreenY - offsetY;
        
        g2d.setColor(new Color(0, 100, 0, 30));
        g2d.setStroke(new BasicStroke(1));
        
        for (int x = startX; x < screenWidth + tileSize; x += tileSize) {
            g2d.drawLine(x, 0, x, screenHeight);
        }
        
        for (int y = startY; y < screenHeight + tileSize; y += tileSize) {
            g2d.drawLine(0, y, screenWidth, y);
        }
    }
    
    private void drawClouds(Graphics2D g2d, Object mainPlayer, int screenWidth, int screenHeight, float zoom) {
        int playerScreenX = screenWidth / 2;
        int playerScreenY = screenHeight / 2;
        
        int bufferZone = 200;
        int playerX = getPlayerX(mainPlayer);
        int playerY = getPlayerY(mainPlayer);
        
        for (int i = 0; i < cloudPositions.length; i++) {
            int cloudX = cloudPositions[i];
            int cloudY = 50 + (i * 80) % 200;
            
            int relativeX = cloudX - playerX;
            int relativeY = cloudY - playerY;
            int screenX = playerScreenX + (int) (relativeX * zoom * 0.3f);
            int screenY = playerScreenY + (int) (relativeY * zoom * 0.3f);
            
            if (screenX > -bufferZone && screenX < screenWidth + bufferZone && 
                screenY > -bufferZone && screenY < screenHeight + bufferZone) {
                drawCloud(g2d, screenX, screenY, 30 + (i % 3) * 10);
            }
        }
    }
    
    private void drawCloud(Graphics2D g2d, int x, int y, int size) {
        g2d.setColor(new Color(255, 255, 255, 120));
        g2d.fillOval(x, y, size, size/2);
        g2d.fillOval(x + size/3, y - size/4, size/2, size/2);
        g2d.fillOval(x + size/2, y, size/2, size/2);
    }
    
    
    private Color getTerrainColor(int noiseValue) {
        if (noiseValue < 20) {
            return new Color(34, 139, 34);
        } else if (noiseValue < 40) {
            return new Color(107, 142, 35);
        } else if (noiseValue < 60) {
            return new Color(139, 69, 19);
        } else if (noiseValue < 80) {
            return new Color(160, 82, 45);
        } else {
            return new Color(210, 180, 140);
        }
    }
    
    private void drawTerrainDetails(Graphics2D g2d, int x, int y, int tileSize, int noiseValue) {
        if (noiseValue < 30) {
            g2d.setColor(new Color(0, 100, 0, 30));
            g2d.fillOval(x + tileSize/4, y + tileSize/4, tileSize/2, tileSize/2);
        } else if (noiseValue > 70) {
            g2d.setColor(new Color(139, 69, 19, 40));
            g2d.fillRect(x + tileSize/3, y + tileSize/3, tileSize/3, tileSize/3);
        }
    }
    
    private void drawBackgroundObjects(Graphics2D g2d, Object mainPlayer, int screenWidth, int screenHeight, float zoom) {
        int playerScreenX = screenWidth / 2;
        int playerScreenY = screenHeight / 2;
        
        int bufferZone = (int) (200 * zoom);
        int playerX = getPlayerX(mainPlayer);
        int playerY = getPlayerY(mainPlayer);
        int mapHeight = 1500;
        
        for (int i = 0; i < treePositions.length; i++) {
            int treeX = treePositions[i];
            int treeY = 100 + (i * 120) % (mapHeight - 200);
            
            int relativeX = treeX - playerX;
            int relativeY = treeY - playerY;
            int screenX = playerScreenX + (int) (relativeX * zoom);
            int screenY = playerScreenY + (int) (relativeY * zoom);
            
            if (screenX > -bufferZone && screenX < screenWidth + bufferZone && 
                screenY > -bufferZone && screenY < screenHeight + bufferZone) {
                drawTree(g2d, screenX, screenY, (int) (20 * zoom));
            }
        }
        
        for (int i = 0; i < rockPositions.length; i++) {
            int rockX = rockPositions[i];
            int rockY = 150 + (i * 90) % (mapHeight - 300);
            
            int relativeX = rockX - playerX;
            int relativeY = rockY - playerY;
            int screenX = playerScreenX + (int) (relativeX * zoom);
            int screenY = playerScreenY + (int) (relativeY * zoom);
            
            if (screenX > -bufferZone && screenX < screenWidth + bufferZone && 
                screenY > -bufferZone && screenY < screenHeight + bufferZone) {
                drawRock(g2d, screenX, screenY, (int) (15 * zoom));
            }
        }
    }
    
    private void drawTree(Graphics2D g2d, int x, int y, int size) {
        g2d.setColor(new Color(139, 69, 19, 180));
        g2d.fillRect(x + size/3, y + size/2, size/3, size/2);
        
        g2d.setColor(new Color(34, 139, 34, 200));
        g2d.fillOval(x, y, size, size);
        g2d.fillOval(x + size/4, y - size/4, size/2, size/2);
        g2d.fillOval(x + size/2, y, size/2, size/2);
    }
    
    private void drawRock(Graphics2D g2d, int x, int y, int size) {
        g2d.setColor(new Color(105, 105, 105, 200));
        g2d.fillOval(x, y, size, size/2);
        g2d.setColor(new Color(128, 128, 128, 150));
        g2d.fillOval(x + size/4, y - size/8, size/2, size/2);
    }
    
    private void drawGrid(Graphics2D g2d, Object mainPlayer, int screenWidth, int screenHeight, float zoom) {
        int tileSize = (int) (32 * zoom);
        int playerScreenX = screenWidth / 2;
        int playerScreenY = screenHeight / 2;
        
        int playerX = getPlayerX(mainPlayer);
        int playerY = getPlayerY(mainPlayer);
        int offsetX = (int) ((playerX % 32) * zoom);
        int offsetY = (int) ((playerY % 32) * zoom);
        
        int bufferZone = tileSize * 2;
        int startX = playerScreenX - offsetX - bufferZone;
        int startY = playerScreenY - offsetY - bufferZone;
        
        g2d.setColor(new Color(0, 100, 0, 60));
        g2d.setStroke(new BasicStroke(1));
        
        for (int x = startX; x < screenWidth + bufferZone; x += tileSize) {
            g2d.drawLine(x, 0, x, screenHeight);
        }
        
        for (int y = startY; y < screenHeight + bufferZone; y += tileSize) {
            g2d.drawLine(0, y, screenWidth, y);
        }
        
        g2d.setColor(new Color(0, 80, 0, 30));
        g2d.setStroke(new BasicStroke(1));
        
        int subTileSize = tileSize / 4;
        for (int x = startX; x < screenWidth + bufferZone; x += subTileSize) {
            g2d.drawLine(x, 0, x, screenHeight);
        }
        
        for (int y = startY; y < screenHeight + bufferZone; y += subTileSize) {
            g2d.drawLine(0, y, screenWidth, y);
        }
    }
    
    private void drawMapBounds(Graphics2D g2d, Object mainPlayer, int screenWidth, int screenHeight, float zoom) {
        int mapWidth = (int) (2000 * zoom);
        int mapHeight = (int) (1500 * zoom);
        
        int playerX = getPlayerX(mainPlayer);
        int playerY = getPlayerY(mainPlayer);
        int mapStartX = screenWidth / 2 - (int) (playerX * zoom);
        int mapStartY = screenHeight / 2 - (int) (playerY * zoom);
        
        g2d.setColor(new Color(139, 69, 19, 200));
        g2d.setStroke(new BasicStroke(6));
        g2d.drawRect(mapStartX, mapStartY, mapWidth, mapHeight);
        
        g2d.setColor(new Color(160, 82, 45, 150));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(mapStartX + 3, mapStartY + 3, mapWidth - 6, mapHeight - 6);
    }
}
