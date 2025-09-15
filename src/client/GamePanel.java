package client;

import common.Chicken;
import common.DroppedItem;
import common.GameConfig;
import common.Player;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import javax.swing.*;

public class GamePanel extends JPanel implements KeyListener {
    private Map<String, Player> players;
    private String playerId;
    private final Map<String, Image> sprites = new HashMap<>();
    private java.util.List<Chicken> chickens = new ArrayList<>();
    private Image chickenSprite;
    private Image chickenHitSprite;
    private java.util.List<DroppedItem> droppedItems = new ArrayList<>();
    private GameClient gameClient;
    private int animationFrame = 0;
    private long lastFrameTime = 0;
    private long attackFeedbackTime = 0;
    private boolean showAttackFeedback = false;
    private final Map<String, Integer> playerAttackFrames = new HashMap<>();
    private long lastAttackTime = 0;
    private final float zoom = 7.0f;
    private SoundManager soundManager;
    private boolean gameStarted = false;
    private InventoryGUI inventoryGUI;
    private JSONDatabase database;
    private String currentPlayerName;
    private int fps = 0;
    private long lastFpsTime = 0;
    private int frameCount = 0;
    private String notificationText = "";
    private long notificationTime = 0;
    private static final long NOTIFICATION_DURATION = 3000;
    private Set<String> previousPlayers = new HashSet<>();
    private JFrame parentFrame;
    private long lastDatabaseUpdate = 0;
    private javax.swing.Timer gameTimer;
    private javax.swing.Timer gameUpdateTimer;

    public GamePanel() {
        setFocusable(true);
        setRequestFocusEnabled(true);
        setDoubleBuffered(true);
        setOpaque(true);
        setBackground(Color.BLACK);
        setIgnoreRepaint(false);
        
        
        startGameTimer();
    }
    
    private void startGameTimer() {
        
        gameTimer = new javax.swing.Timer(16, e -> { 
            if (isDisplayable() && isShowing()) {
                updateGame();
                repaint();
            }
        });
        gameTimer.start();
        
        
        gameUpdateTimer = new javax.swing.Timer(5000, e -> { 
            if (isDisplayable() && isShowing()) {
                loadPlayerDataFromDatabase();
            }
        });
        gameUpdateTimer.start();
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }
    

    public void updateGame() {
        if (!isDisplayable() || !isShowing()) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        
        if (currentTime - lastFrameTime > 100) {
            updateAnimation();
            updateAttackFeedback();
            updateFPS();
            lastFrameTime = currentTime;
        }
        
        
        checkPlayerChanges();
        updateChickens();
        updateDroppedItems();
    }
    
    
    
    private void updateDroppedItems() {
        Player mainPlayer = players.get(playerId);
        if (mainPlayer == null) return;
        
        
        Iterator<DroppedItem> iterator = droppedItems.iterator();
        while (iterator.hasNext()) {
            DroppedItem item = iterator.next();
            
            
            if (item.isExpired()) {
                iterator.remove();
                continue;
            }
            
            
            if (item.isPlayerNearby(mainPlayer, 15)) { 
                
                addToInventory(item.itemName, item.quantity);
                
                
                showNotification("Picked up: " + item.itemName + " x" + item.quantity);
                
                
                iterator.remove();
            }
        }
    }

    public GamePanel(Map<String, Player> players, String playerId, JFrame parentFrame) {
        this();
        this.players = players;
        this.playerId = playerId;
        this.parentFrame = parentFrame;
        this.soundManager = new SoundManager();
        this.gameStarted = true;
        this.inventoryGUI = new InventoryGUI(parentFrame);
        this.database = new JSONDatabase();
        loadSprites();
        loadSounds();
        
      
        createChickens();
        
        loadInventoryFromDatabase();
        addKeyListener(this);
        setFocusable(true);
        requestFocusInWindow();
        
    }

    private void loadSprites() {
        
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

        
        try {
            chickenSprite = new ImageIcon("assets/sprites/enemy/Chicken/sprite_sheet_Idle.png").getImage();
        } catch (Exception e) {
            System.out.println("Failed to load chicken sprite");
        }
        
        
        try {
            chickenHitSprite = new ImageIcon("assets/sprites/enemy/Chicken/sprite_sheet_Hit.png").getImage();
        } catch (Exception e) {
            System.out.println("Failed to load chicken hit sprite");
        }
        
    }

    private void loadSounds() {
    }
    
    private void createChickens() {
        chickens.clear();
        Random random = new Random();
        
        for (int i = 0; i < GameConfig.CHICKEN_COUNT; i++) {
            int x = 100 + random.nextInt(GameConfig.MAP_WIDTH - 200);
            int y = 100 + random.nextInt(GameConfig.MAP_HEIGHT - 200);
            boolean validPosition = false;
            int attempts = 0;
            
            while (!validPosition && attempts < 100) {
                x = 100 + random.nextInt(GameConfig.MAP_WIDTH - 200);
                y = 100 + random.nextInt(GameConfig.MAP_HEIGHT - 200);
                
                validPosition = true;
                
                for (Chicken existingChicken : chickens) {
                    double distance = GameConfig.calculatePreciseDistance(x, y, existingChicken.x, existingChicken.y);
                    if (distance < 80) {
                        validPosition = false;
                        break;
                    }
                }
                
                attempts++;
            }
            
            chickens.add(new Chicken(x, y));
        }
    }
    
    private void updateChickens() {
        for (Chicken chicken : chickens) {
            chicken.update(chickens);
        }
        checkChickenRespawn();
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
            
            
            checkChickenAttack();
        }
    }
    
    private void checkChickenAttack() {
        Player mainPlayer = players.get(playerId);
        if (mainPlayer == null) {
            System.out.println("Main player is null!");
            return;
        }
        
        int attackRange = GameConfig.ATTACK_RANGE;
        System.out.println("Checking chicken attack. Player at: " + mainPlayer.x + ", " + mainPlayer.y + " Attack range: " + attackRange);
        System.out.println("Number of chickens: " + chickens.size());
        
        
        ArrayList<Chicken> chickensCopy = new ArrayList<>(chickens);
        
        for (Chicken chicken : chickensCopy) {
            if (chicken.canBeAttacked()) {
                
                int chickenCenterX = chicken.x + GameConfig.CHICKEN_SIZE / 2;
                int chickenCenterY = chicken.y + GameConfig.CHICKEN_SIZE / 2;
                
                
                int playerScreenCenterX = getWidth() / 2;
                int playerScreenCenterY = getHeight() / 2;
                
                int relativeX = chicken.x - mainPlayer.x;
                int relativeY = chicken.y - mainPlayer.y;
                int chickenScreenX = getWidth() / 2 + (int) (relativeX * zoom);
                int chickenScreenY = getHeight() / 2 + (int) (relativeY * zoom);
                
                int chickenScreenCenterX = chickenScreenX + (int) (GameConfig.CHICKEN_SIZE * zoom / 2);
                int chickenScreenCenterY = chickenScreenY + (int) (GameConfig.CHICKEN_SIZE * zoom / 2);
                
                double worldDistance = GameConfig.calculateTopDownScreenDistance(
                    playerScreenCenterX, playerScreenCenterY,
                    chickenScreenCenterX, chickenScreenCenterY,
                    zoom
                );
                int distance = (int) Math.round(worldDistance);
                
                if (distance <= attackRange) {
                    System.out.println("Attacking chicken! Distance: " + distance + " <= " + attackRange);
                    
                    chicken.takeDamage(1);
                    System.out.println("Chicken health after damage: " + chicken.health);
                    
                    
                    if (gameClient != null) {
                        gameClient.sendChickenAttack(chicken.toString());
                    }
                    
                    
                    if (soundManager != null) {
                        soundManager.playChickenHitSound();
                    }
                    
                    if (!chicken.isAlive) {
                        
                        mainPlayer.addMoney(chicken.getReward());
                        showNotification("+$" + chicken.getReward() + " (Chicken killed!)");
                        
                        
                        createDroppedItems(chicken.x, chicken.y);
                        
                        
                        if (gameClient != null) {
                            gameClient.sendMoneyUpdate(mainPlayer.money);
                        }
                        
                        
                        savePlayerData();
                        lastDatabaseUpdate = System.currentTimeMillis(); 
                    } else {
                        showNotification("Chicken hit!");
                    }
                }
            }
        }
    }
    
    private void createDroppedItems(int x, int y) {
        
        Random random = new Random();
        
        
        if (random.nextDouble() < 0.8) {
            int featherCount = 1 + random.nextInt(3); 
            droppedItems.add(new DroppedItem(x, y, "Feather", featherCount));
        }
        
        
        if (random.nextDouble() < 0.6) {
            int coinCount = 1 + random.nextInt(2); 
            droppedItems.add(new DroppedItem(x, y, "Coin", coinCount));
        }
        
      
        
        System.out.println("Created dropped items at: " + x + ", " + y);
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
        
        
        if (!hasFocus()) {
            requestFocusInWindow();
        }

        Graphics2D g2d = (Graphics2D) g.create();
        try {
            
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
            g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            g2d.setComposite(AlphaComposite.SrcOver);

            drawBackground(g2d);

            
            drawChickens(g2d);
            
            
            
            
            drawDroppedItems(g2d);
            
            synchronized (players) {
                
                java.util.List<Player> otherPlayers = new ArrayList<>();
                Player mainPlayer = null;
                
                for (Player p : players.values()) {
                    if (p.id.equals(playerId)) {
                        mainPlayer = p;
                    } else {
                        otherPlayers.add(p);
                    }
                }
                
                
                otherPlayers.sort((p1, p2) -> Integer.compare(p1.y, p2.y));
                
                
                for (Player p : otherPlayers) {
                    
                    int relativeX = p.x - mainPlayer.x;
                    int relativeY = p.y - mainPlayer.y;
                    int playerScreenX = getWidth() / 2 + (int) (relativeX * zoom);
                    int playerScreenY = getHeight() / 2 + (int) (relativeY * zoom);
                    
                    int scaledSize = (int) (GameConfig.PLAYER_SIZE * zoom);
                    int renderDistance = GameConfig.RENDER_DISTANCE;
                    
                    
                    double preciseDistance = GameConfig.calculatePreciseDistance(
                        mainPlayer.x + GameConfig.PLAYER_SIZE / 2,
                        mainPlayer.y + GameConfig.PLAYER_SIZE / 2,
                        p.x + GameConfig.PLAYER_SIZE / 2,
                        p.y + GameConfig.PLAYER_SIZE / 2
                    );
                    int distance = (int) Math.round(preciseDistance);
                    
                    
                    if (distance > GameConfig.RENDER_DISTANCE) {
                        continue; 
                    }
                    
                    
                    if (GameConfig.DEBUG_DISTANCE) {
                        System.out.println("Other player distance: " + distance + " pixels (RENDER_DISTANCE: " + GameConfig.RENDER_DISTANCE + ")");
                        
                        
                        g2d.setColor(new Color(0, 0, 255, 150)); 
                        g2d.setStroke(new BasicStroke(2));
                        g2d.drawLine(getWidth() / 2, getHeight() / 2, playerScreenX + scaledSize / 2, playerScreenY + scaledSize / 2);
                        
                        
                        g2d.setColor(Color.BLUE);
                        g2d.setFont(new Font("Arial", Font.BOLD, 12));
                        g2d.drawString(distance + "px", playerScreenX, playerScreenY - 5);
                    }
                    
                    drawPlayer(g2d, p);
                }
                
                
                if (mainPlayer != null) {
                    drawPlayer(g2d, mainPlayer);
                }
                
                
                drawPlayerDistanceLines(g2d);
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
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        
        g2d.setColor(new Color(18, 18, 18));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        
        drawFogOfWar(g2d);
        
        
        drawMapBounds(g2d);
    }

    private void drawFogOfWar(Graphics2D g2d) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int renderRadius = GameConfig.RENDER_DISTANCE * 5;
        
        RadialGradientPaint fogPaint = new RadialGradientPaint(
            centerX, centerY, renderRadius,
            new float[]{0.0f, 0.85f, 1.0f},
            new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 70), new Color(0, 0, 0, 75)}
        );
        
        g2d.setPaint(fogPaint);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        

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

        
        g2d.setColor(new Color(0, 100, 0, 120));
        g2d.setStroke(new BasicStroke(2));
        
        for (int x = startX; x < getWidth() + tileSize; x += tileSize) {
            g2d.drawLine(x, 0, x, getHeight());
        }

        for (int y = startY; y < getHeight() + tileSize; y += tileSize) {
            g2d.drawLine(0, y, getWidth(), y);
        }
        
        
        g2d.setColor(new Color(0, 80, 0, 80));
        g2d.setStroke(new BasicStroke(1));
        
        int subTileSize = tileSize / 4;
        for (int x = startX; x < getWidth() + subTileSize; x += subTileSize) {
            g2d.drawLine(x, 0, x, getHeight());
        }

        for (int y = startY; y < getHeight() + subTileSize; y += subTileSize) {
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

        
        g2d.setColor(new Color(139, 69, 19, 200)); 
        g2d.setStroke(new BasicStroke(6));
        g2d.drawRect(mapStartX, mapStartY, mapWidth, mapHeight);
        
        
        g2d.setColor(new Color(160, 82, 45, 150)); 
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(mapStartX + 3, mapStartY + 3, mapWidth - 6, mapHeight - 6);
    }
    
    private void drawPlayerDistanceLines(Graphics2D g2d) {
        Player mainPlayer = players.get(playerId);
        if (mainPlayer == null) return;
        
        int mainPlayerScreenX = getWidth() / 2;
        int mainPlayerScreenY = getHeight() / 2;
        
        
        
        
        for (Player otherPlayer : players.values()) {
            if (!otherPlayer.id.equals(playerId)) {
                int relativeX = otherPlayer.x - mainPlayer.x;
                int relativeY = otherPlayer.y - mainPlayer.y;
                int otherPlayerScreenX = mainPlayerScreenX + (int) (relativeX * zoom);
                int otherPlayerScreenY = mainPlayerScreenY + (int) (relativeY * zoom);
                
                
                g2d.setColor(new Color(100, 150, 255, 80)); 
                g2d.setStroke(new BasicStroke(1));
                g2d.drawLine(mainPlayerScreenX, mainPlayerScreenY, otherPlayerScreenX, otherPlayerScreenY);
                
                
                    double preciseDistance = GameConfig.calculatePreciseDistance(0, 0, relativeX, relativeY);
                    int distance = (int) Math.round(preciseDistance);
                int midX = (mainPlayerScreenX + otherPlayerScreenX) / 2;
                int midY = (mainPlayerScreenY + otherPlayerScreenY) / 2;
                
                
                g2d.setColor(new Color(0, 0, 0, 100));
                g2d.fillRect(midX - 15, midY - 10, 30, 12);
                
                g2d.setColor(new Color(255, 255, 255, 200));
                g2d.setFont(new Font("Arial", Font.BOLD, 8));
                g2d.drawString(distance + "px", midX - 12, midY - 2);
            }
        }
    }
    
    private void drawChickens(Graphics2D g2d) {
        Player mainPlayer = players.get(playerId);
        if (mainPlayer == null) return;
        
        int mainPlayerScreenX = getWidth() / 2;
        int mainPlayerScreenY = getHeight() / 2;
        
        
        int renderDistance = GameConfig.RENDER_DISTANCE;
        int viewLeft = -renderDistance;
        int viewRight = getWidth() + renderDistance;
        int viewTop = -renderDistance;
        int viewBottom = getHeight() + renderDistance;
        
        for (Chicken chicken : chickens) {
            if (!chicken.isAlive || chicken.state.equals("dead")) continue;
            
            int relativeX = chicken.x - mainPlayer.x;
            int relativeY = chicken.y - mainPlayer.y;
            int chickenScreenX = mainPlayerScreenX + (int) (relativeX * zoom);
            int chickenScreenY = mainPlayerScreenY + (int) (relativeY * zoom);
            
            int scaledSize = (int) (GameConfig.CHICKEN_SIZE * zoom);
            
            
          
            int playerScreenCenterX = getWidth() / 2; 
            int playerScreenCenterY = getHeight() / 2;
            
            int chickenScreenCenterX = chickenScreenX + (int) (GameConfig.CHICKEN_SIZE * zoom / 2);
            int chickenScreenCenterY = chickenScreenY + (int) (GameConfig.CHICKEN_SIZE * zoom / 2);
            
            double worldDistance = GameConfig.calculateTopDownScreenDistance(
                playerScreenCenterX, playerScreenCenterY,
                chickenScreenCenterX, chickenScreenCenterY,
                zoom
            );
            int distance = (int) Math.round(worldDistance);
            
            if (GameConfig.DEBUG_DISTANCE) {
                int playerCenterX = mainPlayer.x + GameConfig.PLAYER_SIZE / 2;
                int playerCenterY = mainPlayer.y + GameConfig.PLAYER_SIZE / 2;
                int chickenCenterX = chicken.x + GameConfig.CHICKEN_SIZE / 2;
                int chickenCenterY = chicken.y + GameConfig.CHICKEN_SIZE / 2;
                
                int dx = Math.abs(playerCenterX - chickenCenterX);
                int dy = Math.abs(playerCenterY - chickenCenterY);
                double screenDistance = Math.sqrt(
                    Math.pow(playerScreenCenterX - chickenScreenCenterX, 2) + 
                    Math.pow(playerScreenCenterY - chickenScreenCenterY, 2)
                );
                
                System.out.println("Chicken Debug - World: (" + playerCenterX + "," + playerCenterY + 
                    ") to (" + chickenCenterX + "," + chickenCenterY + 
                    ") DX: " + dx + " DY: " + dy + 
                    " | Screen: (" + playerScreenCenterX + "," + playerScreenCenterY + 
                    ") to (" + chickenScreenCenterX + "," + chickenScreenCenterY + 
                    ") ScreenDist: " + String.format("%.2f", screenDistance) + 
                    " WorldDist: " + String.format("%.2f", worldDistance) + " Rounded: " + distance);
            }
            
            
            if (distance > GameConfig.RENDER_DISTANCE) {
                continue; 
            }
            
            
            if (GameConfig.DEBUG_DISTANCE) {
                System.out.println("Chicken distance: " + distance + " pixels (RENDER_DISTANCE: " + GameConfig.RENDER_DISTANCE + ")");
                
                
                g2d.setColor(new Color(255, 255, 0, 150)); 
                g2d.setStroke(new BasicStroke(2));
                g2d.drawLine(getWidth() / 2, getHeight() / 2, chickenScreenX + scaledSize / 2, chickenScreenY + scaledSize / 2);
                
                
                g2d.setColor(Color.YELLOW);
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                g2d.drawString(distance + "px", chickenScreenX, chickenScreenY - 5);
            }
            
            
            int chickenWorldCenterX = chicken.x + GameConfig.CHICKEN_SIZE / 2;
            int chickenWorldCenterY = chicken.y + GameConfig.CHICKEN_SIZE / 2;
            
            
            Player closestPlayer = mainPlayer;
            int minDistance = Integer.MAX_VALUE;
            
            synchronized (players) {
                for (Player player : players.values()) {
                    int playerCenterX2 = player.x + GameConfig.PLAYER_SIZE / 2;
                    int playerCenterY2 = player.y + GameConfig.PLAYER_SIZE / 2;
                    
                    double preciseDistance2 = GameConfig.calculatePreciseDistance(chickenWorldCenterX, chickenWorldCenterY, playerCenterX2, playerCenterY2);
                    int chickenDistance = (int) Math.round(preciseDistance2);
                    
                    if (chickenDistance < minDistance) {
                        minDistance = chickenDistance;
                        closestPlayer = player;
                    }
                }
            }
            
            int playerWorldCenterX = closestPlayer.x + GameConfig.PLAYER_SIZE / 2;
            int playerWorldCenterY = closestPlayer.y + GameConfig.PLAYER_SIZE / 2;
            
            boolean facingLeft = chickenWorldCenterX > playerWorldCenterX; 
            
   
            if (chicken.state.equals("hit") && chickenHitSprite != null) {
                
                int frameWidth = chickenHitSprite.getWidth(null) / GameConfig.CHICKEN_HIT_FRAMES;
                int frameHeight = chickenHitSprite.getHeight(null);
                
                int srcX = chicken.hitFrame * frameWidth;
                int srcY = 0;
                
                System.out.println("Drawing chicken hit sprite - Frame: " + chicken.hitFrame + " SrcX: " + srcX + " FrameWidth: " + frameWidth + " FacingLeft: " + facingLeft);
                
                
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
                g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
                
                if (facingLeft) {
                    
                    g2d.drawImage(chickenHitSprite,
                            chickenScreenX + scaledSize, chickenScreenY, chickenScreenX, chickenScreenY + scaledSize,
                            srcX, srcY, srcX + frameWidth, srcY + frameHeight,
                            null);
                } else {
                    
                    g2d.drawImage(chickenHitSprite,
                            chickenScreenX, chickenScreenY, chickenScreenX + scaledSize, chickenScreenY + scaledSize,
                            srcX, srcY, srcX + frameWidth, srcY + frameHeight,
                            null);
                }
            } else if (chickenSprite != null) {
                
                int frameWidth = chickenSprite.getWidth(null) / GameConfig.CHICKEN_ANIMATION_FRAMES;
                int frameHeight = chickenSprite.getHeight(null);
                
                int srcX = chicken.animationFrame * frameWidth;
                int srcY = 0;
                
                
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
                g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
                
                if (facingLeft) {
                    
                    g2d.drawImage(chickenSprite,
                            chickenScreenX + scaledSize, chickenScreenY, chickenScreenX, chickenScreenY + scaledSize,
                            srcX, srcY, srcX + frameWidth, srcY + frameHeight,
                            null);
                } else {
                    
                    g2d.drawImage(chickenSprite,
                            chickenScreenX, chickenScreenY, chickenScreenX + scaledSize, chickenScreenY + scaledSize,
                            srcX, srcY, srcX + frameWidth, srcY + frameHeight,
                            null);
                }
            } else {
                
                g2d.setColor(new Color(255, 200, 0, 255)); 
                g2d.fillOval(chickenScreenX, chickenScreenY, scaledSize, scaledSize);
                
                g2d.setColor(new Color(255, 100, 0, 255)); 
                g2d.fillOval(chickenScreenX + 2, chickenScreenY + 2, scaledSize - 4, scaledSize - 4);
            }
            
            
            g2d.setColor(new Color(0, 0, 0, 40));
            int shadowWidth = scaledSize / 2;
            int shadowHeight = 3;
            int shadowX = chickenScreenX + (scaledSize - shadowWidth) / 2;
            int shadowY = chickenScreenY + scaledSize - 1;
            g2d.fillOval(shadowX, shadowY, shadowWidth, shadowHeight);
            
            
            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            String chickenName = "Chicken";
            int nameWidth = g2d.getFontMetrics().stringWidth(chickenName);
            int nameX = chickenScreenX + (scaledSize - nameWidth) / 2;
            int nameY = chickenScreenY - 5;
            
            
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillRect(nameX - 2, nameY - 12, nameWidth + 4, 12);
            
            
            g2d.setColor(new Color(255, 255, 255, 255));
            g2d.drawString(chickenName, nameX, nameY);
            
            
        }
    }
    
    
    private void drawDroppedItems(Graphics2D g2d) {
        Player mainPlayer = players.get(playerId);
        if (mainPlayer == null) return;
        
        int mainPlayerScreenX = getWidth() / 2;
        int mainPlayerScreenY = getHeight() / 2;
        
        
        int renderDistance = GameConfig.RENDER_DISTANCE;
        int viewLeft = -renderDistance;
        int viewRight = getWidth() + renderDistance;
        int viewTop = -renderDistance;
        int viewBottom = getHeight() + renderDistance;
        
        for (DroppedItem item : droppedItems) {
            int relativeX = item.x - mainPlayer.x;
            int relativeY = item.y - mainPlayer.y;
            int itemScreenX = mainPlayerScreenX + (int) (relativeX * zoom);
            int itemScreenY = mainPlayerScreenY + (int) (relativeY * zoom);
            
            
            double preciseDistance = GameConfig.calculatePreciseDistance(
                mainPlayer.x + GameConfig.PLAYER_SIZE / 2,
                mainPlayer.y + GameConfig.PLAYER_SIZE / 2,
                item.x + 8, 
                item.y + 8
            );
            int distance = (int) Math.round(preciseDistance);
            
            
            if (distance > GameConfig.RENDER_DISTANCE) {
                continue; 
            }
            
            
            if (GameConfig.DEBUG_DISTANCE) {
                System.out.println("Dropped item distance: " + distance + " pixels (RENDER_DISTANCE: " + GameConfig.RENDER_DISTANCE + ")");
                
                
                g2d.setColor(new Color(0, 255, 0, 150)); 
                g2d.setStroke(new BasicStroke(2));
                g2d.drawLine(getWidth() / 2, getHeight() / 2, itemScreenX + 8, itemScreenY + 8);
                
                
                g2d.setColor(Color.GREEN);
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                g2d.drawString(distance + "px", itemScreenX, itemScreenY - 5);
            }
            
            
            item.draw(g2d, itemScreenX, itemScreenY, zoom);
        }
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

    public void showNotification(String message) {
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

            
            if (g instanceof Graphics2D g2d) {
                g2d.setColor(new Color(0, 0, 0, 80));
                int shadowWidth = scaledSize / 3;
                int shadowHeight = 5;
                int shadowX = screenX + (scaledSize - shadowWidth) / 2;
                int shadowY = screenY + scaledSize - 90;
                g2d.fillOval(shadowX, shadowY, shadowWidth, shadowHeight);
            }
            
            g.drawImage(img,
                    screenX, screenY, screenX + scaledSize, screenY + scaledSize,
                    srcX, srcY, srcX + frameWidth, srcY + frameHeight,
                    null);

            
            if (g instanceof Graphics2D g2d) {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            }
            
            

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
            
            
        }
    }

    public boolean checkPlayerCollision(Player player, int newX, int newY) {
        synchronized (players) {
            for (Player otherPlayer : players.values()) {
                if (!otherPlayer.id.equals(player.id)) {
                    
                    int minDistance = 7 ; 
                    
                    double preciseDistance = GameConfig.calculatePreciseDistance(newX, newY, otherPlayer.x, otherPlayer.y);
                    int distance = (int) Math.round(preciseDistance);
                    
                    if (distance < minDistance) {
                        return true; 
                    }
                }
            }
        }
        
        

        return false; 
    }

    public Map<String, Player> getPlayers() {
        return players;
    }
    
    public GameClient getGameClient() {
        return gameClient;
    }
    
    public void setGameClient(GameClient gameClient) {
        this.gameClient = gameClient;
    }
    
    public void setDatabase(JSONDatabase database) {
        this.database = database;
    }
    
    
    @Override
    public void keyPressed(KeyEvent e) {
        System.out.println("Key pressed: " + e.getKeyCode() + " (I = " + KeyEvent.VK_I + ") Focus: " + hasFocus());
        if (e.getKeyCode() == KeyEvent.VK_I) {
            System.out.println("I pressed - toggling inventory");
            e.consume(); 
            
            
            loadPlayerDataFromDatabase();
            
            inventoryGUI.toggleVisibility();
            
            requestFocusInWindow();
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            System.out.println("SPACE pressed - checking shop interaction");
            e.consume();
            
            
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        
    }
    
    
    public JSONDatabase getDatabase() {
        return database;
    }
    
    public String getCurrentPlayerName() {
        return currentPlayerName;
    }
    
    
    public void addToInventory(String itemName, int quantity) {
        inventoryGUI.addItem(itemName, quantity);
        savePlayerData(); 
        lastDatabaseUpdate = System.currentTimeMillis(); 
    }
    
    public void removeFromInventory(String itemName, int quantity) {
        inventoryGUI.removeItem(itemName, quantity);
        savePlayerData(); 
        lastDatabaseUpdate = System.currentTimeMillis(); 
    }
    
    public int getInventoryQuantity(String itemName) {
        return inventoryGUI.getItemQuantity(itemName);
    }
    
    
    public void setCurrentPlayerName(String playerName) {
        this.currentPlayerName = playerName;
        loadPlayerData();
    }
    
    private void loadPlayerData() {
        if (currentPlayerName != null && database != null) {
            JSONDatabase.PlayerData playerData = database.getPlayerData(currentPlayerName);
            
            
            Player mainPlayer = players.get(playerId);
            if (mainPlayer != null) {
                mainPlayer.money = playerData.money;
                System.out.println("Updated money to: " + mainPlayer.money);
            }
            
            
            inventoryGUI.loadItems(playerData.inventory);
            
            
            repaint();
            
            System.out.println("Loaded player data for: " + currentPlayerName);
            System.out.println("Money: " + playerData.money);
            System.out.println("Inventory: " + playerData.inventory);
        }
    }
    
    private void loadInventoryFromDatabase() {
        if (database != null && currentPlayerName != null) {
            
            JSONDatabase.PlayerData playerData = database.getPlayerData(currentPlayerName);
            if (playerData != null) {
                inventoryGUI.loadItems(playerData.inventory);
                System.out.println("Loaded inventory from database for player: " + currentPlayerName);
                System.out.println("Inventory: " + playerData.inventory);
            } else {
                System.out.println("No player data found for: " + currentPlayerName);
            }
        }
    }
    
    public void loadPlayerDataFromDatabase() {
        if (database != null && currentPlayerName != null) {
            JSONDatabase.PlayerData playerData = database.getPlayerData(currentPlayerName);
            if (playerData != null) {
                
                Player mainPlayer = players.get(playerId);
                if (mainPlayer != null) {
                    mainPlayer.money = playerData.money;
                    System.out.println("Updated money to: " + mainPlayer.money);
                }
                
                
                inventoryGUI.loadItems(playerData.inventory);
                
                
                repaint();
                
                System.out.println("Loaded player data from database for: " + currentPlayerName);
                System.out.println("Money: " + playerData.money);
                System.out.println("Inventory: " + playerData.inventory);
            } else {
                System.out.println("No player data found for: " + currentPlayerName);
            }
        }
    }
    
    public void savePlayerData() {
        if (currentPlayerName != null && database != null) {
            Player mainPlayer = players.get(playerId);
            if (mainPlayer != null) {
                JSONDatabase.PlayerData playerData = new JSONDatabase.PlayerData();
                playerData.playerName = currentPlayerName;
                playerData.money = mainPlayer.money;
                playerData.level = 1; 
                playerData.experience = 0; 
                
                
                playerData.inventory.putAll(inventoryGUI.getAllItems());
                
                database.savePlayerData(currentPlayerName, playerData);
                System.out.println("Saved player data for: " + currentPlayerName);
            }
        }
    }
    
    public void updateChickensFromServer(String chickensData) {
        try {
            System.out.println("Received chickens data: " + chickensData);
            chickens.clear();
            String[] all = chickensData.split(";");
            for (String c : all) {
                if (!c.isEmpty()) {
                    Chicken chicken = Chicken.fromString(c);
                    chickens.add(chicken);
                    System.out.println("Added chicken: " + chicken.x + ", " + chicken.y + " Alive: " + chicken.isAlive);
                }
            }
            System.out.println("Total chickens loaded: " + chickens.size());
        } catch (Exception e) {
            System.out.println("Error updating chickens from server: " + e.getMessage());
        }
    }
    
    private void checkChickenRespawn() {
        long currentTime = System.currentTimeMillis();
        for (Chicken chicken : chickens) {
            if (!chicken.isAlive && chicken.state.equals("dead")) {
                if (currentTime - chicken.deathTime > GameConfig.CHICKEN_RESPAWN_TIME) {
                    System.out.println("Respawning chicken at: " + chicken.originalX + ", " + chicken.originalY);
                    chicken.respawn();
                    
                    
                    if (gameClient != null) {
                        gameClient.sendChickenAttack(chicken.toString());
                    }
                }
            }
        }
    }
    
    public void updateChickenFromServer(String chickenData) {
        try {
            Chicken serverChicken = Chicken.fromString(chickenData);
            
            
            for (Chicken localChicken : chickens) {
                double preciseDistance = GameConfig.calculatePreciseDistance(localChicken.x, localChicken.y, serverChicken.x, serverChicken.y);
                int distance = (int) Math.round(preciseDistance);
                
                if (distance < GameConfig.CHICKEN_SIZE) {
                    
                    localChicken.health = serverChicken.health;
                    localChicken.isAlive = serverChicken.isAlive;
                    localChicken.state = serverChicken.state;
                    localChicken.hitFrame = serverChicken.hitFrame;
                    localChicken.deathTime = serverChicken.deathTime;
                    localChicken.lastHitTime = serverChicken.lastHitTime;
                    
                    System.out.println("Updated local chicken: " + localChicken.x + ", " + localChicken.y + " Health: " + localChicken.health + " Alive: " + localChicken.isAlive + " State: " + localChicken.state);
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Error updating chicken from server: " + e.getMessage());
        }
    }

    public void cleanup() {
        if (soundManager != null) {
            soundManager.cleanup();
        }
    }

}
