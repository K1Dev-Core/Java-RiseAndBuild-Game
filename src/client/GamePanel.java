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
    private long teleportEffectTime = 0;
    private boolean showTeleportEffect = false;
    private boolean isInsidePortal = false;
    private Portal currentPortal = null;
    private boolean gridInitialized = false;
    
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
        for (int i = 0; i < GameConfig.PORTAL_POSITIONS.length; i++) {
            int[] pos = GameConfig.PORTAL_POSITIONS[i];
            String portalId = "Portal" + (char)('A' + i);
            String targetId = "Portal" + (char)('A' + (i == 0 ? 1 : 0));
            
            Portal portal = new Portal(portalId, pos[0], pos[1], pos[2], pos[3], targetId);
            portals.put(portalId, portal);
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
        
        g.setColor(new Color(30, 30, 30));
        g.fillRect(0, 0, getWidth(), getHeight());
        
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
        drawMapBoundaries(g);
        drawPortals(g);
        drawTeleportEffect(g);
        drawTeleportStatus(g);
        checkPortalTeleport();
    }
    
    private void drawInstructions(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("WASD - Move | Mouse Click - Attack | SPACE - Teleport | ESC - Exit", 20, 30);
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
        if (!gridInitialized) {
            gridInitialized = true;
        }
        
        g.setColor(new Color(30, 30, 30));
        g.fillRect(0, 0, getWidth(), getHeight());
    }
    
    private void drawGridSquares(Graphics g) {
        Player mainPlayer = players.get(playerId);
        if (mainPlayer == null) return;
        
        int gridSize = (int)(GameConfig.GRID_SIZE * zoom);
        int startX = getWidth() / 2 - (int)((mainPlayer.x % GameConfig.GRID_SIZE) * zoom);
        int startY = getHeight() / 2 - (int)((mainPlayer.y % GameConfig.GRID_SIZE) * zoom);
        
        int mapGridWidth = GameConfig.MAP_WIDTH / GameConfig.GRID_SIZE;
        int mapGridHeight = GameConfig.MAP_HEIGHT / GameConfig.GRID_SIZE;
        
        for (int x = startX; x < getWidth() + gridSize; x += gridSize) {
            for (int y = startY; y < getHeight() + gridSize; y += gridSize) {
                int worldX = (x - startX) / gridSize;
                int worldY = (y - startY) / gridSize;
                
                if (worldX >= 0 && worldX < mapGridWidth && worldY >= 0 && worldY < mapGridHeight) {
                    if ((worldX + worldY) % 2 == 0) {
                        g.setColor(new Color(50, 50, 50));
                    } else {
                        g.setColor(new Color(40, 40, 40));
                    }
                    
                    g.fillRect(x, y, gridSize, gridSize);
                    g.setColor(new Color(70, 70, 70));
                    g.drawRect(x, y, gridSize, gridSize);
                }
            }
        }
        
        for (Portal portal : portals.values()) {
            int portalGridX = portal.x / GameConfig.GRID_SIZE;
            int portalGridY = portal.y / GameConfig.GRID_SIZE;
            
            for (int px = 0; px < GameConfig.PORTAL_GRID_SIZE; px++) {
                for (int py = 0; py < GameConfig.PORTAL_GRID_SIZE; py++) {
                    int worldX = portalGridX + px;
                    int worldY = portalGridY + py;
                    
                    if (worldX >= 0 && worldX < mapGridWidth && worldY >= 0 && worldY < mapGridHeight) {
                        int screenX = startX + worldX * gridSize;
                        int screenY = startY + worldY * gridSize;
                        
                        g.setColor(new Color(100, 50, 100));
                        g.fillRect(screenX, screenY, gridSize, gridSize);
                        g.setColor(new Color(150, 100, 150));
                        g.drawRect(screenX, screenY, gridSize, gridSize);
                    }
                }
            }
        }
    }
    
    private void drawPortals(Graphics g) {
        Player mainPlayer = players.get(playerId);
        if (mainPlayer == null) return;
        
        for (Portal portal : portals.values()) {
            portal.updateAnimation();
            
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
            
            if (portal.isPlayerNear(mainPlayer)) {
                g.setColor(Color.YELLOW);
                g.drawOval(screenX - 10, screenY - 10, GameConfig.PORTAL_SIZE + 20, GameConfig.PORTAL_SIZE + 20);
                
                if (portal.isPlayerInside(mainPlayer)) {
                    g.setColor(Color.CYAN);
                    g.drawOval(screenX - 5, screenY - 5, GameConfig.PORTAL_SIZE + 10, GameConfig.PORTAL_SIZE + 10);
                    
                    if (portal.canTeleport()) {
                        g.setColor(Color.GREEN);
                        g.drawString("PRESS SPACE TO TELEPORT", screenX, screenY + GameConfig.PORTAL_SIZE + 20);
                    } else {
                        g.setColor(Color.RED);
                        g.drawString("COOLDOWN", screenX, screenY + GameConfig.PORTAL_SIZE + 20);
                    }
                } else {
                    g.setColor(Color.ORANGE);
                    g.drawString("GET CLOSER TO TELEPORT", screenX, screenY + GameConfig.PORTAL_SIZE + 20);
                }
            }
            
            int collisionDistance = GameConfig.PORTAL_SIZE / 2 + GameConfig.PLAYER_SIZE / 2 + 20;
            if (distance < collisionDistance) {
                g.setColor(Color.RED);
                g.drawOval(screenX - 15, screenY - 15, GameConfig.PORTAL_SIZE + 30, GameConfig.PORTAL_SIZE + 30);
                g.setColor(Color.WHITE);
                g.drawString("COLLISION ZONE", screenX, screenY + GameConfig.PORTAL_SIZE + 40);
            }
        }
    }
    
    private void checkPortalTeleport() {
        Player mainPlayer = players.get(playerId);
        if (mainPlayer != null) {
            isInsidePortal = false;
            currentPortal = null;
            
            for (Portal portal : portals.values()) {
                boolean isInside = portal.isPlayerInside(mainPlayer);
                if (isInside) {
                    isInsidePortal = true;
                    currentPortal = portal;
                    break;
                }
            }
        }
    }
    
    public void checkPlayerMovement() {
        Player mainPlayer = players.get(playerId);
        if (mainPlayer != null) {
            for (Portal portal : portals.values()) {
                int playerCenterX = mainPlayer.x + GameConfig.PLAYER_SIZE / 2;
                int playerCenterY = mainPlayer.y + GameConfig.PLAYER_SIZE / 2;
                int portalCenterX = portal.x + GameConfig.PORTAL_SIZE / 2;
                int portalCenterY = portal.y + GameConfig.PORTAL_SIZE / 2;
                
                int distance = (int) Math.sqrt(Math.pow(playerCenterX - portalCenterX, 2) + Math.pow(playerCenterY - portalCenterY, 2));
                int minDistance = GameConfig.PORTAL_SIZE / 2 + GameConfig.PLAYER_SIZE / 2 + 20;
                
                if (distance < minDistance) {
                    int dx = playerCenterX - portalCenterX;
                    int dy = playerCenterY - portalCenterY;
                    
                    if (dx != 0 || dy != 0) {
                        double angle = Math.atan2(dy, dx);
                        int pushX = (int) (Math.cos(angle) * minDistance);
                        int pushY = (int) (Math.sin(angle) * minDistance);
                        
                        mainPlayer.setPosition(portalCenterX + pushX - GameConfig.PLAYER_SIZE / 2, 
                                             portalCenterY + pushY - GameConfig.PLAYER_SIZE / 2);
                    }
                }
            }
        }
    }
    
    private void preventPlayerFromWalkingThroughPortal(Player player) {
        for (Portal portal : portals.values()) {
            int playerGridX = player.x / GameConfig.GRID_SIZE;
            int playerGridY = player.y / GameConfig.GRID_SIZE;
            int portalGridX = portal.x / GameConfig.GRID_SIZE;
            int portalGridY = portal.y / GameConfig.GRID_SIZE;
            
            if (playerGridX >= portalGridX && playerGridX < portalGridX + GameConfig.PORTAL_GRID_SIZE &&
                playerGridY >= portalGridY && playerGridY < portalGridY + GameConfig.PORTAL_GRID_SIZE) {
                
                int pushX = playerGridX < portalGridX + GameConfig.PORTAL_GRID_SIZE / 2 ? 
                    portalGridX - 1 : portalGridX + GameConfig.PORTAL_GRID_SIZE;
                int pushY = playerGridY < portalGridY + GameConfig.PORTAL_GRID_SIZE / 2 ? 
                    portalGridY - 1 : portalGridY + GameConfig.PORTAL_GRID_SIZE;
                
                player.x = pushX * GameConfig.GRID_SIZE;
                player.y = pushY * GameConfig.GRID_SIZE;
            }
        }
    }
    
    public void teleportPlayer() {
        if (isInsidePortal && currentPortal != null && currentPortal.canTeleport()) {
            Player mainPlayer = players.get(playerId);
            if (mainPlayer != null) {
                currentPortal.teleportPlayer(mainPlayer);
                showTeleportEffect = true;
                teleportEffectTime = System.currentTimeMillis();
            }
        }
    }
    
    private void drawTeleportEffect(Graphics g) {
        if (showTeleportEffect) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - teleportEffectTime > 1000) {
                showTeleportEffect = false;
            } else {
                g.setColor(new Color(0, 0, 0, 150));
                g.fillRect(0, 0, getWidth(), getHeight());
                
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 48));
                g.drawString("TELEPORTING...", getWidth() / 2 - 150, getHeight() / 2);
            }
        }
    }
    
    private void drawTeleportStatus(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        
        String status = "Teleport Status: ";
        if (isInsidePortal && currentPortal != null) {
            status += "INSIDE " + currentPortal.id;
            if (currentPortal.canTeleport()) {
                status += " - READY";
                g.setColor(Color.GREEN);
            } else {
                status += " - COOLDOWN";
                g.setColor(Color.RED);
            }
        } else {
            status += "NOT INSIDE";
            g.setColor(Color.YELLOW);
        }
        
        g.drawString(status, 20, 100);
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