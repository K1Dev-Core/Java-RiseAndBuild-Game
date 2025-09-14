package client;

import common.GameConfig;
import common.Player;
import java.awt.*;
import java.util.*;
import javax.swing.*;

public class GamePanel extends JPanel {
    private Map<String, Player> players;
    private String playerId;
    private final Map<String, Image> sprites = new HashMap<>();
    private int animationFrame = 0;
    private long lastFrameTime = 0;
    private long attackFeedbackTime = 0;
    private boolean showAttackFeedback = false;
    private final Map<String, Integer> playerAttackFrames = new HashMap<>();
    private long lastAttackTime = 0;
    private final float zoom = 5.0f;
    private boolean gridInitialized = false;
    private SoundManager soundManager;
    
    public GamePanel() {
        setFocusable(true);
        setRequestFocusEnabled(true);
    }
    
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
    

    public GamePanel(Map<String, Player> players, String playerId) {
        this();
        this.players = players;
        this.playerId = playerId;
        this.soundManager = new SoundManager();
        loadSprites();
        loadSounds();
    }

    private void loadSprites() {
        String[] states = {"idle", "run", "attack1", "attack2"};
        String[] dirs = {"up", "down", "left", "right"};
        
        for (String st : states) {
            for (String d : dirs) {
                String key = st + "_" + d;
                String path = "assets/sprites/player/male/" + key + ".png";
                
                try {
                    Image img = new ImageIcon(path).getImage();
                    sprites.put(key, img);
                } catch (Exception e) {
                    System.out.println("Failed to load sprite: " + key);
                }
            }
        }
        
    }
    
    
    private void loadSounds() {
        // ลบการใช้เสียงออก
    }

    public void showAttackFeedback() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime > GameConfig.ATTACK_DURATION + GameConfig.ATTACK_COOLDOWN) {
            showAttackFeedback = true;
            attackFeedbackTime = currentTime;
            lastAttackTime = currentTime;
            
            // เล่นเสียง slash เมื่อโจมตี
            if (soundManager != null) {
                soundManager.playSlashSound();
            }
        }
    }
    
    
    
    private void drawCooldownBar(Graphics g) {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastAttackTime;
        long totalCooldown = GameConfig.ATTACK_DURATION + GameConfig.ATTACK_COOLDOWN;
        
        if (elapsed < totalCooldown) {
            synchronized(players) {
                for (Player p : players.values()) {
                    if (p.id.equals(playerId)) {
                        int barWidth = (int)(40 * zoom);
                        int barHeight = (int)(4 * zoom);
                        int scaledSize = (int)(GameConfig.PLAYER_SIZE * zoom);
                        int x = getWidth() / 2 - barWidth / 2;
                        int y = getHeight() / 2 + scaledSize / 2 + 20;
                        
                        g.setColor(Color.GRAY);
                        g.fillRect(x, y, barWidth, barHeight);
                        
                        float progress = (float) elapsed / totalCooldown;
                        int fillWidth = (int) (barWidth * progress);
                        
                        g.setColor(Color.RED);
                        g.fillRect(x, y, fillWidth, barHeight);
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        updateAnimation();
        updateAttackFeedback();
        
        drawGridBackground(g);
        drawGridSquares(g);
        
        synchronized(players) {
            for (Player p : players.values()) {
                drawPlayer(g, p);
            }
        }
        
        if (showAttackFeedback) {
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("ATTACK!", 50, 50);
        }
        
        drawCooldownBar(g);
        drawInstructions(g);
        drawPlayerCoordinates(g);
    }
    
    private void drawInstructions(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("WASD - Move | Mouse Click - Attack | ESC - Exit", 20, 30);
    }
    
    private void drawPlayerCoordinates(Graphics g) {
        synchronized(players) {
            for (Player p : players.values()) {
                if (p.id.equals(playerId)) {
                    g.setColor(Color.CYAN);
                    g.setFont(new Font("Arial", Font.BOLD, 18));
                    String coords = "Position: X=" + p.x + " Y=" + p.y;
                    int textWidth = g.getFontMetrics().stringWidth(coords);
                    g.drawString(coords, getWidth() - textWidth - 20, 30);
                    break;
                }
            }
        }
    }
    
    
    private void drawGridBackground(Graphics g) {
        if (!gridInitialized) {
            gridInitialized = true;
        }
        
        g.setColor(new Color(30, 30, 30));
        g.fillRect(0, 0, getWidth(), getHeight());
    }
    
    private void drawGridSquares(Graphics g) {
        Player mainPlayer = players.get(playerId);
        if (mainPlayer == null) return;
        
        // คำนวณตำแหน่งของแผนที่บนหน้าจอ
        int mapLeft = getWidth() / 2 - (int)(mainPlayer.x * zoom);
        int mapTop = getHeight() / 2 - (int)(mainPlayer.y * zoom);
        
        // ขนาดช่องตาราง 64x64 พิกเซล
        int gridSize = 64;
        
        // คำนวณขอบเขตของตารางที่จะวาด
        int startX = (mapLeft / gridSize) * gridSize;
        int startY = (mapTop / gridSize) * gridSize;
        int endX = startX + getWidth() + gridSize;
        int endY = startY + getHeight() + gridSize;
        
        // วาดเส้นตารางแนวตั้ง
        g.setColor(new Color(60, 60, 60, 100)); // สีเทาอ่อนโปร่งใส
        for (int x = startX; x <= endX; x += gridSize) {
            int screenX = x - mapLeft;
            if (screenX >= 0 && screenX <= getWidth()) {
                g.drawLine(screenX, 0, screenX, getHeight());
            }
        }
        
        // วาดเส้นตารางแนวนอน
        for (int y = startY; y <= endY; y += gridSize) {
            int screenY = y - mapTop;
            if (screenY >= 0 && screenY <= getHeight()) {
                g.drawLine(0, screenY, getWidth(), screenY);
            }
        }
        
        // วาดขอบเขตแผนที่
        g.setColor(new Color(255, 100, 100, 150)); // สีแดงโปร่งใส
        int mapRight = mapLeft + (int)(GameConfig.MAP_WIDTH * zoom);
        int mapBottom = mapTop + (int)(GameConfig.MAP_HEIGHT * zoom);
        
        // วาดเส้นขอบแผนที่
        if (mapLeft < getWidth() && mapRight > 0) {
            g.drawLine(mapLeft, 0, mapLeft, getHeight());
            g.drawLine(mapRight, 0, mapRight, getHeight());
        }
        
        if (mapTop < getHeight() && mapBottom > 0) {
            g.drawLine(0, mapTop, getWidth(), mapTop);
            g.drawLine(0, mapBottom, getWidth(), mapBottom);
        }
        
        // วาดข้อมูลตาราง
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        String gridInfo = "Grid: 64x64 | Map: " + GameConfig.MAP_WIDTH + "x" + GameConfig.MAP_HEIGHT;
        g.drawString(gridInfo, 20, getHeight() - 40);
    }
    
    
    
    public void checkPlayerMovement() {
        // ลบการตรวจสอบ Portal ออก
    }
    
    
    
    
    
    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime > (1000 / GameConfig.ANIMATION_SPEED)) {
            synchronized(players) {
                for (Player p : players.values()) {
                    if (p.state.equals("attack2")) {
                        int currentFrame = playerAttackFrames.getOrDefault(p.id, 0);
                        if (currentFrame < GameConfig.ANIMATION_FRAMES - 1) {
                            currentFrame++;
                            playerAttackFrames.put(p.id, currentFrame);
                        }
                    } else {
                        playerAttackFrames.remove(p.id);
                    }
                }
                
                animationFrame = (animationFrame + 1) % GameConfig.ANIMATION_FRAMES;
            }
            lastFrameTime = currentTime;
        }
    }
    
    private void updateAttackFeedback() {
        if (showAttackFeedback) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - attackFeedbackTime > 500) {
                showAttackFeedback = false;
            }
        }
    }
    
    private void drawPlayer(Graphics g, Player p) {
        String key = p.state + "_" + p.direction;
        Image img = sprites.get(key);
        
        if (p.id.equals(playerId)) {
            // จัดการเสียง footsteps
            if (p.state.equals("run")) {
                if (soundManager != null && !soundManager.isFootstepPlaying()) {
                    soundManager.startFootstepSound();
                }
            } else {
                if (soundManager != null && soundManager.isFootstepPlaying()) {
                    soundManager.stopFootstepSound();
                }
            }
        }
        
        if (img != null) {
            int frameWidth = img.getWidth(null) / GameConfig.ANIMATION_FRAMES;
            int frameHeight = img.getHeight(null);
            
            int currentFrame;
            if (p.state.equals("attack2")) {
                currentFrame = playerAttackFrames.getOrDefault(p.id, 0);
            } else {
                currentFrame = animationFrame;
            }
            
            int srcX = currentFrame * frameWidth;
            int srcY = 0;
            
            int scaledSize = (int)(GameConfig.PLAYER_SIZE * zoom);
            int screenX, screenY;
            
            if (p.id.equals(playerId)) {
                screenX = getWidth() / 2 - scaledSize / 2;
                screenY = getHeight() / 2 - scaledSize / 2;
            } else {
                Player mainPlayer = players.get(playerId);
                if (mainPlayer != null) {
                    int relativeX = p.x - mainPlayer.x;
                    int relativeY = p.y - mainPlayer.y;
                    screenX = getWidth() / 2 + (int)(relativeX * zoom) - scaledSize / 2;
                    screenY = getHeight() / 2 + (int)(relativeY * zoom) - scaledSize / 2;
                } else {
                    screenX = getWidth() / 2;
                    screenY = getHeight() / 2;
                }
            }
            
            g.drawImage(img, 
                screenX, screenY, screenX + scaledSize, screenY + scaledSize,
                srcX, srcY, srcX + frameWidth, srcY + frameHeight,
                null);
                
            
            g.setColor(Color.WHITE);
            String status = p.id + " - " + p.state + " F:" + currentFrame;
            if (p.state.equals("attack2")) {
                status += " (ATTACKING)";
            } else if (!p.canAttack) {
                status += " (COOLDOWN)";
            }
            g.drawString(status, screenX, screenY - 5);
            
            if (p.id.equals(playerId)) {
                g.setColor(Color.YELLOW);
                g.setFont(new Font("Arial", Font.BOLD, 14));
                g.drawString("X: " + p.x + " Y: " + p.y, screenX, screenY - 20);
            }
        } else {
            int scaledSize = (int)(GameConfig.PLAYER_SIZE * zoom);
            int screenX, screenY;
            
            if (p.id.equals(playerId)) {
                screenX = getWidth() / 2 - scaledSize / 2;
                screenY = getHeight() / 2 - scaledSize / 2;
            } else {
                Player mainPlayer = players.get(playerId);
                if (mainPlayer != null) {
                    int relativeX = p.x - mainPlayer.x;
                    int relativeY = p.y - mainPlayer.y;
                    screenX = getWidth() / 2 + (int)(relativeX * zoom) - scaledSize / 2;
                    screenY = getHeight() / 2 + (int)(relativeY * zoom) - scaledSize / 2;
                } else {
                    screenX = getWidth() / 2;
                    screenY = getHeight() / 2;
                }
            }
            
            g.setColor(Color.BLUE);
            g.fillRect(screenX, screenY, scaledSize, scaledSize);
            g.setColor(Color.WHITE);
            g.drawString(p.id + " - " + p.state, screenX, screenY - 5);
        }
    }
    
    public void cleanup() {
        if (soundManager != null) {
            soundManager.cleanup();
        }
    }
    
}