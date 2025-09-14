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
    private final float zoom = 1.0f;
    private SoundManager soundManager;
    private boolean gameStarted = false;
    private boolean needsRepaint = false;
    private int fps = 0;
    private long lastFpsTime = 0;
    private int frameCount = 0;
    
    public GamePanel() {
        setFocusable(true);
        setRequestFocusEnabled(true);
        setDoubleBuffered(true);
        setOpaque(true);
        setBackground(Color.BLACK);
        setIgnoreRepaint(false);
        
        System.setProperty("sun.java2d.opengl", "false");
        System.setProperty("sun.java2d.d3d", "false");
        System.setProperty("sun.java2d.noddraw", "true");
    }
    
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
    
    public void updateGame() {
        if (!isDisplayable() || !isShowing()) {
            return;
        }
        updateAnimation();
        updateAttackFeedback();
        updateFPS();
    }
    
    public void requestRepaint() {
        needsRepaint = true;
    }
    

    public GamePanel(Map<String, Player> players, String playerId) {
        this();
        this.players = players;
        this.playerId = playerId;
        this.soundManager = new SoundManager();
        this.gameStarted = true;
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
    }

    public void showAttackFeedback() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime > GameConfig.ATTACK_DURATION + GameConfig.ATTACK_COOLDOWN) {
            showAttackFeedback = true;
            attackFeedbackTime = currentTime;
            lastAttackTime = currentTime;
            
            if (soundManager != null) {
                soundManager.playSlashSound();
            }
        }
    }
    
    
    
    private void drawCooldownBar(Graphics2D g2d) {
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
                        
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                        g2d.setColor(new Color(100, 100, 100, 255));
                        g2d.fillRect(x, y, barWidth, barHeight);
                        
                        float progress = (float) elapsed / totalCooldown;
                        int fillWidth = (int) (barWidth * progress);
                        
                        g2d.setColor(new Color(255, 100, 100, 255));
                        g2d.fillRect(x, y, fillWidth, barHeight);
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
            g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            
            g2d.setComposite(AlphaComposite.SrcOver);
            
            drawBackground(g2d);
            
            synchronized(players) {
                for (Player p : players.values()) {
                    drawPlayer(g2d, p);
                }
            }
            
      
            
            drawCooldownBar(g2d);
            drawPlayerCoordinates(g2d);
            drawFPS(g2d);
        } finally {
            g2d.dispose();
        }
    }
    
    
    private void drawPlayerCoordinates(Graphics2D g2d) {
        synchronized(players) {
            for (Player p : players.values()) {
                if (p.id.equals(playerId)) {
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                    g2d.setColor(new Color(255, 255, 255, 255));
                    g2d.setFont(new Font("Arial", Font.BOLD, 10));
                    String coords = "X: " + p.x + " Y: " + p.y;
                    int textWidth = g2d.getFontMetrics().stringWidth(coords);
                    g2d.drawString(coords, getWidth() - textWidth - 20, getHeight() - 40);
                    break;
                }
            }
        }
    }
    
    private void drawFPS(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g2d.setColor(new Color(0, 255, 0, 255));
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        String fpsText = "FPS: " + fps;
        g2d.drawString(fpsText, 20, getHeight() - 20);
    }
    
    private void drawBackground(Graphics2D g2d) {
        // วาดพื้นหลังสีเขียวเข้ม
        g2d.setColor(new Color(20, 40, 20));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        // คำนวณตำแหน่งกล้องตามตัวละคร
        Player mainPlayer = players.get(playerId);
        if (mainPlayer != null) {
            int cameraX = (int)(mainPlayer.x * zoom) - getWidth() / 2;
            int cameraY = (int)(mainPlayer.y * zoom) - getHeight() / 2;
            
            // วาดตารางที่เคลื่อนไหวตามกล้อง
            int gridSize = 64;
            int startX = (cameraX / gridSize) * gridSize;
            int startY = (cameraY / gridSize) * gridSize;
            
            for (int x = startX; x < startX + getWidth() + gridSize; x += gridSize) {
                for (int y = startY; y < startY + getHeight() + gridSize; y += gridSize) {
                    int screenX = x - cameraX;
                    int screenY = y - cameraY;
                    
                    // สลับสีระหว่างช่อง
                    if (((x / gridSize) + (y / gridSize)) % 2 == 0) {
                        g2d.setColor(new Color(40, 80, 40));  // สีเขียว
                    } else {
                        g2d.setColor(new Color(60, 50, 30));  // สีน้ำตาล
                    }
                    g2d.fillRect(screenX, screenY, gridSize, gridSize);
                    
                    // วาดขอบตาราง
                    g2d.setColor(new Color(30, 30, 30));
                    g2d.drawRect(screenX, screenY, gridSize, gridSize);
                }
            }
        } else {
            // วาดตารางธรรมดาเมื่อไม่มีตัวละคร
            int gridSize = 64;
            for (int x = 0; x < getWidth(); x += gridSize) {
                for (int y = 0; y < getHeight(); y += gridSize) {
                    if (((x / gridSize) + (y / gridSize)) % 2 == 0) {
                        g2d.setColor(new Color(40, 80, 40));
                    } else {
                        g2d.setColor(new Color(60, 50, 30));
                    }
                    g2d.fillRect(x, y, gridSize, gridSize);
                    
                    g2d.setColor(new Color(30, 30, 30));
                    g2d.drawRect(x, y, gridSize, gridSize);
                }
            }
        }
    }
    
    
    
    
    
    
    public void checkPlayerMovement() {
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
                
                Player mainPlayer = players.get(playerId);
                if (mainPlayer != null && soundManager != null && gameStarted) {
                    if (mainPlayer.state.equals("run")) {
                        if (!soundManager.isFootstepPlaying()) {
                            soundManager.startFootstepSound();
                        }
                    } else {
                        if (soundManager.isFootstepPlaying()) {
                            soundManager.stopFootstepSound();
                        }
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
    
    private void updateFPS() {
        long currentTime = System.currentTimeMillis();
        frameCount++;
        
        if (currentTime - lastFpsTime >= 1000) {
            fps = frameCount;
            frameCount = 0;
            lastFpsTime = currentTime;
        }
    }
    
    private void drawPlayer(Graphics g, Player p) {
        String key = p.state + "_" + p.direction;
        Image img = sprites.get(key);
        
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
            
            if (g instanceof Graphics2D g2d) {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
                g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            }
            
            g.drawImage(img, 
                screenX, screenY, screenX + scaledSize, screenY + scaledSize,
                srcX, srcY, srcX + frameWidth, srcY + frameHeight,
                null);
                
            
            if (p.id.equals(playerId)) {
                if (g instanceof Graphics2D g2d) {
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                    g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
                }
                g.setColor(new Color(255, 255, 255, 255));
                g.setFont(new Font("Arial", Font.BOLD, 10));
                String status = p.id + " - " + p.state + " F:" + currentFrame;
                if (p.state.equals("attack2")) {
                    status += " (ATTACKING)";
                } else if (!p.canAttack) {
                    status += " (COOLDOWN)";
                }
                int textWidth = g.getFontMetrics().stringWidth(status);
                g.drawString(status, getWidth() - textWidth - 20, getHeight() - 20);
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
            
            g.setColor(new Color(0, 0, 255, 255));
            g.fillRect(screenX, screenY, scaledSize, scaledSize);
            if (g instanceof Graphics2D g2d) {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            }
            g.setColor(new Color(255, 255, 255, 255));
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.drawString(p.id + " - " + p.state, screenX, screenY - 5);
        }
    }
    
    public boolean checkPlayerCollision(Player player, int newX, int newY) {
        // ไม่มีการตรวจสอบ collision - วิ่งได้เรื่อยๆ
        return false;
    }
    
    public void cleanup() {
        if (soundManager != null) {
            soundManager.cleanup();
        }
    }
    
}