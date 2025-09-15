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
    private final float zoom = 7.0f;
    private SoundManager soundManager;
    private boolean gameStarted = false;
    private int fps = 0;
    private long lastFpsTime = 0;
    private int frameCount = 0;
    private String notificationText = "";
    private long notificationTime = 0;
    private static final long NOTIFICATION_DURATION = 3000;
    private Set<String> previousPlayers = new HashSet<>();

    public GamePanel() {
        setFocusable(true);
        setRequestFocusEnabled(true);
        setDoubleBuffered(true);
        setOpaque(true);
        setBackground(Color.BLACK);
        setIgnoreRepaint(false);
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
        checkPlayerChanges();
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
        // Load player sprites
        String[] states = { "idle", "run", "attack1", "attack2" };
        String[] dirs = { "up", "down", "left", "right" };

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
            synchronized (players) {
                for (Player p : players.values()) {
                    if (p.id.equals(playerId)) {
                        int barWidth = (int) (40 * zoom);
                        int barHeight = (int) (4 * zoom);
                        int scaledSize = (int) (GameConfig.PLAYER_SIZE * zoom);
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
            g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            g2d.setComposite(AlphaComposite.SrcOver);

            drawBackground(g2d);

            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

            synchronized (players) {
                for (Player p : players.values()) {
                    drawPlayer(g2d, p);
                }
            }

            drawCooldownBar(g2d);
            drawPlayerCoordinates(g2d);
            drawMoney(g2d);
            drawFPS(g2d);
            drawNotification(g2d);
        } finally {
            g2d.dispose();
        }
    }

    private void drawPlayerCoordinates(Graphics2D g2d) {
        synchronized (players) {
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

    private void drawMoney(Graphics2D g2d) {
        synchronized (players) {
            for (Player p : players.values()) {
                if (p.id.equals(playerId)) {
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                    g2d.setColor(new Color(255, 215, 0, 255));
                    g2d.setFont(new Font("Arial", Font.BOLD, 14));
                    String moneyText = "Money: " + p.money;
                    g2d.drawString(moneyText, 20, 30);
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

    private void drawNotification(Graphics2D g2d) {
        long currentTime = System.currentTimeMillis();

        if (!notificationText.isEmpty() && (currentTime - notificationTime) < NOTIFICATION_DURATION) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

            g2d.setColor(new Color(255, 255, 255, 255));
            g2d.setFont(new Font("Arial", Font.BOLD, 16));

            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(notificationText);
            int x = getWidth() - textWidth - 20;
            int y = 40;

            g2d.setColor(new Color(255, 255, 255, 255));
            g2d.drawString(notificationText, x, y);
        } else if (!notificationText.isEmpty() && (currentTime - notificationTime) >= NOTIFICATION_DURATION) {
            notificationText = "";
        }
    }

    private void drawBackground(Graphics2D g2d) {
        g2d.setColor(new Color(25, 45, 25));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        drawMapBounds(g2d);
    }

    private void drawGrid(Graphics2D g2d) {
        Player mainPlayer = players.get(playerId);
        if (mainPlayer == null)
            return;

        int tileSize = (int) (GameConfig.TILE_SIZE * zoom);
        int playerScreenX = getWidth() / 2;
        int playerScreenY = getHeight() / 2;

        int offsetX = (int) ((mainPlayer.x % GameConfig.TILE_SIZE) * zoom);
        int offsetY = (int) ((mainPlayer.y % GameConfig.TILE_SIZE) * zoom);

        int startX = playerScreenX - offsetX;
        int startY = playerScreenY - offsetY;

        g2d.setColor(new Color(60, 80, 60, 150));
        g2d.setStroke(new BasicStroke(1));

        for (int x = startX; x < getWidth() + tileSize; x += tileSize) {
            g2d.drawLine(x, 0, x, getHeight());
        }

        for (int y = startY; y < getHeight() + tileSize; y += tileSize) {
            g2d.drawLine(0, y, getWidth(), y);
        }

    }

    private void drawMapBounds(Graphics2D g2d) {
        Player mainPlayer = players.get(playerId);
        if (mainPlayer == null)
            return;

        int mapWidth = (int) (GameConfig.MAP_WIDTH * zoom);
        int mapHeight = (int) (GameConfig.MAP_HEIGHT * zoom);

        int mapStartX = getWidth() / 2 - (int) (mainPlayer.x * zoom);
        int mapStartY = getHeight() / 2 - (int) (mainPlayer.y * zoom);

        g2d.setColor(new Color(255, 255, 0, 200));
        g2d.setStroke(new BasicStroke(4));

        g2d.drawRect(mapStartX, mapStartY, mapWidth, mapHeight);
    }

    public void checkPlayerMovement() {
    }

    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime > (1000 / GameConfig.ANIMATION_SPEED)) {
            synchronized (players) {
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

    private void checkPlayerChanges() {
        synchronized (players) {
            Set<String> currentPlayers = new HashSet<>(players.keySet());

            for (String playerId : currentPlayers) {
                if (!previousPlayers.contains(playerId) && !playerId.equals(this.playerId)) {
                    showNotification("Player  " + playerId + " has joined the game");
                }
            }

            for (String playerId : previousPlayers) {
                if (!currentPlayers.contains(playerId) && !playerId.equals(this.playerId)) {
                    showNotification("Player " + playerId + " has left the game");
                }
            }

            previousPlayers = new HashSet<>(currentPlayers);
        }
    }

    private void showNotification(String message) {
        notificationText = message;
        notificationTime = System.currentTimeMillis();
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

            int scaledSize = (int) (GameConfig.PLAYER_SIZE * zoom);
            int screenX, screenY;

            if (p.id.equals(playerId)) {
                screenX = getWidth() / 2 - scaledSize / 2;
                screenY = getHeight() / 2 - scaledSize / 2;
            } else {
                Player mainPlayer = players.get(playerId);
                if (mainPlayer != null) {
                    int relativeX = p.x - mainPlayer.x;
                    int relativeY = p.y - mainPlayer.y;
                    screenX = getWidth() / 2 + (int) (relativeX * zoom) - scaledSize / 2;
                    screenY = getHeight() / 2 + (int) (relativeY * zoom) - scaledSize / 2;
                } else {
                    screenX = getWidth() / 2;
                    screenY = getHeight() / 2;
                }
            }

            if (g instanceof Graphics2D g2d) {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
                g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                        RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
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
                    g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                            RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
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
            int scaledSize = (int) (GameConfig.PLAYER_SIZE * zoom);
            int screenX, screenY;

            if (p.id.equals(playerId)) {
                screenX = getWidth() / 2 - scaledSize / 2;
                screenY = getHeight() / 2 - scaledSize / 2;
            } else {
                Player mainPlayer = players.get(playerId);
                if (mainPlayer != null) {
                    int relativeX = p.x - mainPlayer.x;
                    int relativeY = p.y - mainPlayer.y;
                    screenX = getWidth() / 2 + (int) (relativeX * zoom) - scaledSize / 2;
                    screenY = getHeight() / 2 + (int) (relativeY * zoom) - scaledSize / 2;
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
        return false; // No collision detection needed
    }

    public Map<String, Player> getPlayers() {
        return players;
    }

    public void cleanup() {
        if (soundManager != null) {
            soundManager.cleanup();
        }
    }

}