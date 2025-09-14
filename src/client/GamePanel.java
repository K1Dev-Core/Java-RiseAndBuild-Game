package client;

import common.*;
import java.applet.AudioClip;
import java.awt.*;
import java.io.File;
import java.util.*;
import javax.swing.*;

public class GamePanel extends JPanel {
    private Map<String, Player> players;
    private String playerId;
    private Map<String, Image> sprites = new HashMap<>();
    private Map<String, Portal> portals = new HashMap<>();
    private Image portalSprite;
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
    private float zoom = 5.0f;
    
    public GamePanel() {
        setFocusable(true);
        setRequestFocusEnabled(true);
    }
    
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
    
    public Map<String, Portal> getPortals() {
        return portals;
    }

    public GamePanel(Map<String, Player> players, String playerId) {
        this();
        this.players = players;
        this.playerId = playerId;
        loadSprites();
        loadSounds();
        createPortals();
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
        
        try {
            portalSprite = javax.imageio.ImageIO.read(new File("assets/sprites/obj/portal/Isometric_Portal.png"));
        } catch (Exception e) {
            System.out.println("Could not load portal sprite: " + e.getMessage());
        }
    }
    
    private void createPortals() {
        Portal portalA = new Portal("PortalA", 1000, 1000, 3000, 3000, "PortalB");
        Portal portalB = new Portal("PortalB", 3000, 3000, 1000, 1000, "PortalA");
        
        portals.put("PortalA", portalA);
        portals.put("PortalB", portalB);
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
        drawPlayerCoordinates(g);
        drawMapBoundaries(g);
        drawPortals(g);
        checkPortalTeleport();
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
    
    private void drawMapBoundaries(Graphics g) {
        g.setColor(Color.RED);
        
        Player mainPlayer = players.get(playerId);
        if (mainPlayer != null) {
            int mapLeft = getWidth() / 2 - (int)(mainPlayer.x * zoom);
            int mapTop = getHeight() / 2 - (int)(mainPlayer.y * zoom);
            int mapRight = mapLeft + (int)(GameConfig.MAP_WIDTH * zoom);
            int mapBottom = mapTop + (int)(GameConfig.MAP_HEIGHT * zoom);
            
            if (mapLeft < getWidth() && mapRight > 0) {
                g.drawLine(mapLeft, 0, mapLeft, getHeight());
                g.drawLine(mapRight, 0, mapRight, getHeight());
            }
            
            if (mapTop < getHeight() && mapBottom > 0) {
                g.drawLine(0, mapTop, getWidth(), mapTop);
                g.drawLine(0, mapBottom, getWidth(), mapBottom);
            }
        }
        
        g.setColor(Color.ORANGE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Map: " + GameConfig.MAP_WIDTH + "x" + GameConfig.MAP_HEIGHT, 20, getHeight() - 20);
    }
    
    private void drawGridBackground(Graphics g) {
        Player mainPlayer = players.get(playerId);
        if (mainPlayer != null) {
            int gridSize = (int)(20 * zoom);
            
            int startX = getWidth() / 2 - (int)((mainPlayer.x % gridSize) * zoom);
            int startY = getHeight() / 2 - (int)((mainPlayer.y % gridSize) * zoom);
            
            for (int x = startX; x < getWidth() + gridSize; x += gridSize) {
                g.setColor(new Color(80, 80, 80));
                g.drawLine(x, 0, x, getHeight());
            }
            
            for (int y = startY; y < getHeight() + gridSize; y += gridSize) {
                g.setColor(new Color(80, 80, 80));
                g.drawLine(0, y, getWidth(), y);
            }
            
            g.setColor(new Color(120, 120, 120));
            for (int x = startX; x < getWidth() + gridSize; x += gridSize * 5) {
                g.drawLine(x, 0, x, getHeight());
            }
            
            for (int y = startY; y < getHeight() + gridSize; y += gridSize * 5) {
                g.drawLine(0, y, getWidth(), y);
            }
        }
    }
    
    private void drawPortals(Graphics g) {
        for (Portal portal : portals.values()) {
            portal.updateAnimation();
            
            Player mainPlayer = players.get(playerId);
            if (mainPlayer != null) {
                int relativeX = portal.x - mainPlayer.x;
                int relativeY = portal.y - mainPlayer.y;
                int screenX = getWidth() / 2 + (int)(relativeX * zoom) - GameConfig.PORTAL_SIZE / 2;
                int screenY = getHeight() / 2 + (int)(relativeY * zoom) - GameConfig.PORTAL_SIZE / 2;
                
                if (portalSprite != null) {
                    int frameWidth = portalSprite.getWidth(null) / GameConfig.PORTAL_ANIMATION_FRAMES;
                    int frameHeight = portalSprite.getHeight(null);
                    int srcX = portal.animationFrame * frameWidth;
                    int srcY = 0;
                    
                    g.drawImage(portalSprite, 
                        screenX, screenY, screenX + GameConfig.PORTAL_SIZE, screenY + GameConfig.PORTAL_SIZE,
                        srcX, srcY, srcX + frameWidth, srcY + frameHeight,
                        null);
                } else {
                    g.setColor(Color.MAGENTA);
                    g.fillOval(screenX, screenY, GameConfig.PORTAL_SIZE, GameConfig.PORTAL_SIZE);
                }
                
                g.setColor(Color.WHITE);
                g.drawString(portal.id, screenX, screenY - 10);
            }
        }
    }
    
    private void checkPortalTeleport() {
        Player mainPlayer = players.get(playerId);
        if (mainPlayer != null) {
            for (Portal portal : portals.values()) {
                if (portal.isPlayerNear(mainPlayer)) {
                    portal.teleportPlayer(mainPlayer);
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
    
}