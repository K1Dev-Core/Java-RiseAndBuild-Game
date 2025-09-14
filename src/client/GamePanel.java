package client;

import common.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import java.applet.AudioClip;
import java.io.File;

public class GamePanel extends JPanel {
    private Map<String, Player> players;
    private String playerId;
    private Map<String, Image> sprites = new HashMap<>();
    private int animationFrame = 0;
    private long lastFrameTime = 0;
    private long attackFeedbackTime = 0;
    private boolean showAttackFeedback = false;
    private String lastPlayerState = "";
    private int attackAnimationFrame = 0;
    private Map<String, Integer> playerAttackFrames = new HashMap<>();
    private AudioClip slashSound;
    private AudioClip footstepSound;
    private long lastAttackTime = 0;
    private boolean isPlayingFootstep = false;
    private int cameraX = 0;
    private int cameraY = 0;
    private float zoom = 1.5f;
    
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
        loadSprites();
        loadSounds();
    }

    private void loadSprites() {
        String[] states = {"idle", "run", "attack1", "attack2"};
        String[] dirs = {"up", "down", "left", "right"};
        
        for (String st : states) {
            for (String d : dirs) {
                String key = st + "_" + d;
                String path = "assets/sprites/male/" + key + ".png";
                
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
        try {
            File slashFile = new File("./assets/sounds/slash1.wav");
            if (slashFile.exists()) {
                slashSound = java.applet.Applet.newAudioClip(slashFile.toURI().toURL());
            }
            
            File footstepFile = new File("./assets/sounds/footsteps.wav");
            if (footstepFile.exists()) {
                footstepSound = java.applet.Applet.newAudioClip(footstepFile.toURI().toURL());
            }
        } catch (Exception e) {
            System.out.println("Could not load sounds: " + e.getMessage());
        }
    }

    public void showAttackFeedback() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttackTime > GameConfig.ATTACK_DURATION + GameConfig.ATTACK_COOLDOWN) {
            showAttackFeedback = true;
            attackFeedbackTime = currentTime;
            lastAttackTime = currentTime;
            playSlashSound();
        }
    }
    
    private void playSlashSound() {
        if (slashSound != null) {
            slashSound.play();
        }
    }
    
    private void playFootstepSound() {
        if (footstepSound != null && !isPlayingFootstep) {
            footstepSound.loop();
            isPlayingFootstep = true;
        }
    }
    
    private void stopFootstepSound() {
        if (footstepSound != null && isPlayingFootstep) {
            footstepSound.stop();
            isPlayingFootstep = false;
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
                        int screenX = (int)((p.x - cameraX) * zoom);
                        int screenY = (int)((p.y - cameraY) * zoom);
                        int scaledSize = (int)(GameConfig.PLAYER_SIZE * zoom);
                        int x = screenX + (scaledSize - barWidth) / 2;
                        int y = screenY + (int)(50 * zoom);
                        
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
        updateCamera();
        updateAnimation();
        updateAttackFeedback();
        
        g.setColor(new Color(40, 40, 40));
        g.fillRect(0, 0, getWidth(), getHeight());
        
        drawGridBackground(g);
        
        
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
    }
    
    private void drawInstructions(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("WASD - Move | Mouse Click - Attack | ESC - Exit", 20, 30);
    }
    
    private void drawGridBackground(Graphics g) {
        g.setColor(new Color(60, 60, 60));
        int gridSize = (int)(50 * zoom);
        
        int startX = (int)(-cameraX * zoom) % gridSize;
        int startY = (int)(-cameraY * zoom) % gridSize;
        
        for (int x = startX; x < getWidth() + gridSize; x += gridSize) {
            g.drawLine(x, 0, x, getHeight());
        }
        
        for (int y = startY; y < getHeight() + gridSize; y += gridSize) {
            g.drawLine(0, y, getWidth(), y);
        }
    }
    
    private void updateCamera() {
        synchronized(players) {
            for (Player p : players.values()) {
                if (p.id.equals(playerId)) {
                    cameraX = p.x - getWidth() / 2;
                    cameraY = p.y - getHeight() / 2;
                    break;
                }
            }
        }
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
            if (p.state.equals("run")) {
                playFootstepSound();
            } else {
                stopFootstepSound();
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
            
            int screenX = (int)((p.x - cameraX) * zoom);
            int screenY = (int)((p.y - cameraY) * zoom);
            int scaledSize = (int)(GameConfig.PLAYER_SIZE * zoom);
            
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
                g.setFont(new Font("Arial", Font.BOLD, 12));
                g.drawString("X: " + p.x + " Y: " + p.y, screenX, screenY + scaledSize + 15);
            }
        } else {
            int screenX = (int)((p.x - cameraX) * zoom);
            int screenY = (int)((p.y - cameraY) * zoom);
            int scaledSize = (int)(GameConfig.PLAYER_SIZE * zoom);
            g.setColor(Color.BLUE);
            g.fillRect(screenX, screenY, scaledSize, scaledSize);
            g.setColor(Color.WHITE);
            g.drawString(p.id + " - " + p.state, screenX, screenY - 5);
        }
    }
    
}