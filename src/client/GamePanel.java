package client;

import common.Chicken;
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
        updateChickens();
    }

    public GamePanel(Map<String, Player> players, String playerId) {
        this();
        this.players = players;
        this.playerId = playerId;
        this.soundManager = new SoundManager();
        this.gameStarted = true;
        this.inventoryGUI = new InventoryGUI();
        this.database = new JSONDatabase();
        loadSprites();
        loadSounds();
        addKeyListener(this);
        setFocusable(true);
        requestFocusInWindow();
        // ไม่สร้างไก่ที่นี่ เพราะจะรับจากเซิร์ฟเวอร์
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

        // Load chicken sprite
        try {
            chickenSprite = new ImageIcon("assets/sprites/enemy/Chicken/sprite_sheet_Idle.png").getImage();
        } catch (Exception e) {
            System.out.println("Failed to load chicken sprite");
        }
        
        // Load chicken hit sprite
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
            // สร้างไก่ในพื้นที่เล็กๆ ใกล้กัน
            int centerX = GameConfig.MAP_WIDTH / 2;
            int centerY = GameConfig.MAP_HEIGHT / 2;
            int areaSize = 200; // พื้นที่ 200x200 pixels
            
            int x = centerX + random.nextInt(areaSize) - areaSize/2;
            int y = centerY + random.nextInt(areaSize) - areaSize/2;
            
            // ตรวจสอบขอบเขต
            x = Math.max(0, Math.min(x, GameConfig.MAP_WIDTH - GameConfig.CHICKEN_SIZE));
            y = Math.max(0, Math.min(y, GameConfig.MAP_HEIGHT - GameConfig.CHICKEN_SIZE));
            
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
            
            // ตรวจสอบการโจมตีไก่
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
        
        for (Chicken chicken : chickens) {
            if (chicken.canBeAttacked()) {
                // คำนวณจุดกลางของไก่
                int chickenCenterX = chicken.x + GameConfig.CHICKEN_SIZE / 2;
                int chickenCenterY = chicken.y + GameConfig.CHICKEN_SIZE / 2;
                
                // คำนวณจุดกลางของผู้เล่น
                int playerCenterX = mainPlayer.x + GameConfig.PLAYER_SIZE / 2;
                int playerCenterY = mainPlayer.y + GameConfig.PLAYER_SIZE / 2;
                
                // ตรวจสอบการโจมตีเฉพาะทิศทางตรง (ซ้าย-ขวา-บน-ล่าง)
                int dx = Math.abs(playerCenterX - chickenCenterX);
                int dy = Math.abs(playerCenterY - chickenCenterY);
                
                // ตรวจสอบว่าไก่อยู่ในทิศทางตรงหรือไม่ (ไม่ใช่แนวเฉียง)
                boolean isDirectDirection = (dx == 0 && dy <= attackRange) || (dy == 0 && dx <= attackRange);
                
                System.out.println("Chicken at: " + chicken.x + ", " + chicken.y + " DX: " + dx + " DY: " + dy + " Direct: " + isDirectDirection);
                
                if (isDirectDirection) {
                    System.out.println("Attacking chicken! DX: " + dx + " DY: " + dy + " <= " + attackRange);
                    // โจมตีไก่ได้
                    chicken.takeDamage(1);
                    System.out.println("Chicken health after damage: " + chicken.health);
                    
                    // ส่งข้อมูลการโจมตีไก่ไปยังเซิร์ฟเวอร์
                    if (gameClient != null) {
                        gameClient.sendChickenAttack(chicken.toString());
                    }
                    
                    // เล่นเสียง chicken-hit
                    if (soundManager != null) {
                        soundManager.playChickenHitSound();
                    }
                    
                    if (!chicken.isAlive) {
                        // ไก่ตาย ให้เงินผู้เล่น
                        mainPlayer.addMoney(chicken.getReward());
                        showNotification("+$" + chicken.getReward() + " (Chicken killed!)");
                        
                        // เพิ่มไอเท็มในคลังเก็บของ
                        addToInventory("Chicken Meat", 1);
                        addToInventory("Feather", 2);
                        
                        // ส่งข้อมูลเงินไปยัง server
                        if (gameClient != null) {
                            gameClient.sendMoneyUpdate(mainPlayer.money);
                        }
                    } else {
                        showNotification("Chicken hit!");
                    }
                }
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
        
        // ตรวจสอบว่า GamePanel มี focus หรือไม่
        if (!hasFocus()) {
            requestFocusInWindow();
        }

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

            // วาดไก่ก่อน (เลเยอร์ล่าง)
            drawChickens(g2d);
            
            synchronized (players) {
                // วาดผู้เล่นคนอื่นก่อน (ไม่รวมผู้เล่นหลัก)
                java.util.List<Player> otherPlayers = new ArrayList<>();
                Player mainPlayer = null;
                
                for (Player p : players.values()) {
                    if (p.id.equals(playerId)) {
                        mainPlayer = p;
                    } else {
                        otherPlayers.add(p);
                    }
                }
                
                // เรียงผู้เล่นคนอื่นตามตำแหน่ง Y
                otherPlayers.sort((p1, p2) -> Integer.compare(p1.y, p2.y));
                
                // วาดผู้เล่นคนอื่น
                for (Player p : otherPlayers) {
                    drawPlayer(g2d, p);
                }
                
                // วาดผู้เล่นหลักสุดท้าย (เลเยอร์สูงสุด)
                if (mainPlayer != null) {
                    drawPlayer(g2d, mainPlayer);
                }
                
                // วาดเส้นแสดงระยะห่างระหว่างผู้เล่น
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
        // พื้นหลังหลักสำหรับมุมมอง top-down
        g2d.setColor(new Color(34, 139, 34)); // สีเขียวเข้มสำหรับพื้นหญ้า
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        // วาดกริดเพื่อให้ดูเป็นมิตรกับมุมมอง top-down
        drawGrid(g2d);
        
        // วาดขอบเขตแผนที่
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

        // วาดกริดหลัก (เส้นหนา)
        g2d.setColor(new Color(0, 100, 0, 120));
        g2d.setStroke(new BasicStroke(2));
        
        for (int x = startX; x < getWidth() + tileSize; x += tileSize) {
            g2d.drawLine(x, 0, x, getHeight());
        }

        for (int y = startY; y < getHeight() + tileSize; y += tileSize) {
            g2d.drawLine(0, y, getWidth(), y);
        }
        
        // วาดกริดย่อย (เส้นบาง)
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

        // วาดขอบเขตแผนที่แบบ top-down
        g2d.setColor(new Color(139, 69, 19, 200)); // สีน้ำตาลเข้ม
        g2d.setStroke(new BasicStroke(6));
        g2d.drawRect(mapStartX, mapStartY, mapWidth, mapHeight);
        
        // วาดขอบเขตด้านใน
        g2d.setColor(new Color(160, 82, 45, 150)); // สีน้ำตาลอ่อน
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(mapStartX + 3, mapStartY + 3, mapWidth - 6, mapHeight - 6);
    }
    
    private void drawPlayerDistanceLines(Graphics2D g2d) {
        Player mainPlayer = players.get(playerId);
        if (mainPlayer == null) return;
        
        int mainPlayerScreenX = getWidth() / 2;
        int mainPlayerScreenY = getHeight() / 2;
        
        g2d.setColor(new Color(255, 255, 0, 100)); // สีเหลืองโปร่งใส
        g2d.setStroke(new BasicStroke(1));
        
        for (Player otherPlayer : players.values()) {
            if (!otherPlayer.id.equals(playerId)) {
                int relativeX = otherPlayer.x - mainPlayer.x;
                int relativeY = otherPlayer.y - mainPlayer.y;
                int otherPlayerScreenX = mainPlayerScreenX + (int) (relativeX * zoom);
                int otherPlayerScreenY = mainPlayerScreenY + (int) (relativeY * zoom);
                
                // วาดเส้นเชื่อมระหว่างผู้เล่น
                g2d.drawLine(mainPlayerScreenX, mainPlayerScreenY, otherPlayerScreenX, otherPlayerScreenY);
                
                // วาดระยะห่าง
                int distance = GameConfig.calculateTopDownDistance(0, 0, relativeX, relativeY);
                int midX = (mainPlayerScreenX + otherPlayerScreenX) / 2;
                int midY = (mainPlayerScreenY + otherPlayerScreenY) / 2;
                
                g2d.setColor(new Color(255, 255, 255, 200));
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                g2d.drawString(distance + "px", midX, midY);
                g2d.setColor(new Color(255, 255, 0, 100));
            }
        }
    }
    
    private void drawChickens(Graphics2D g2d) {
        Player mainPlayer = players.get(playerId);
        if (mainPlayer == null) return;
        
        int mainPlayerScreenX = getWidth() / 2;
        int mainPlayerScreenY = getHeight() / 2;
        
        for (Chicken chicken : chickens) {
            if (!chicken.isAlive || chicken.state.equals("dead")) continue;
            
            int relativeX = chicken.x - mainPlayer.x;
            int relativeY = chicken.y - mainPlayer.y;
            int chickenScreenX = mainPlayerScreenX + (int) (relativeX * zoom);
            int chickenScreenY = mainPlayerScreenY + (int) (relativeY * zoom);
            
            int scaledSize = (int) (GameConfig.CHICKEN_SIZE * zoom);
            
            // คำนวณทิศทางจากไก่ไปยังผู้เล่นที่อยู่ใกล้ที่สุด
            int chickenWorldCenterX = chicken.x + GameConfig.CHICKEN_SIZE / 2;
            int chickenWorldCenterY = chicken.y + GameConfig.CHICKEN_SIZE / 2;
            
            // หาผู้เล่นที่อยู่ใกล้ที่สุด
            Player closestPlayer = mainPlayer;
            int minDistance = Integer.MAX_VALUE;
            
            synchronized (players) {
                for (Player player : players.values()) {
                    int playerCenterX = player.x + GameConfig.PLAYER_SIZE / 2;
                    int playerCenterY = player.y + GameConfig.PLAYER_SIZE / 2;
                    
                    int distance = GameConfig.calculateTopDownDistance(chickenWorldCenterX, chickenWorldCenterY, playerCenterX, playerCenterY);
                    
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestPlayer = player;
                    }
                }
            }
            
            int playerWorldCenterX = closestPlayer.x + GameConfig.PLAYER_SIZE / 2;
            int playerWorldCenterY = closestPlayer.y + GameConfig.PLAYER_SIZE / 2;
            
            boolean facingLeft = chickenWorldCenterX > playerWorldCenterX; // ไก่อยู่ทางขวาของผู้เล่น = หันซ้าย
            
            System.out.println("Chicken facing player: " + closestPlayer.id + " Distance: " + minDistance + " FacingLeft: " + facingLeft);
            
            if (chicken.state.equals("hit") && chickenHitSprite != null) {
                // วาด Hit sprite ของไก่
                int frameWidth = chickenHitSprite.getWidth(null) / GameConfig.CHICKEN_HIT_FRAMES;
                int frameHeight = chickenHitSprite.getHeight(null);
                
                int srcX = chicken.hitFrame * frameWidth;
                int srcY = 0;
                
                System.out.println("Drawing chicken hit sprite - Frame: " + chicken.hitFrame + " SrcX: " + srcX + " FrameWidth: " + frameWidth + " FacingLeft: " + facingLeft);
                
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                
                if (facingLeft) {
                    // พลิกภาพแนวนอน (หันซ้าย)
                    g2d.drawImage(chickenHitSprite,
                            chickenScreenX + scaledSize, chickenScreenY, chickenScreenX, chickenScreenY + scaledSize,
                            srcX, srcY, srcX + frameWidth, srcY + frameHeight,
                            null);
                } else {
                    // วาดปกติ (หันขวา)
                    g2d.drawImage(chickenHitSprite,
                            chickenScreenX, chickenScreenY, chickenScreenX + scaledSize, chickenScreenY + scaledSize,
                            srcX, srcY, srcX + frameWidth, srcY + frameHeight,
                            null);
                }
            } else if (chickenSprite != null) {
                // วาด sprite ปกติของไก่
                int frameWidth = chickenSprite.getWidth(null) / GameConfig.CHICKEN_ANIMATION_FRAMES;
                int frameHeight = chickenSprite.getHeight(null);
                
                int srcX = chicken.animationFrame * frameWidth;
                int srcY = 0;
                
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                
                if (facingLeft) {
                    // พลิกภาพแนวนอน (หันซ้าย)
                    g2d.drawImage(chickenSprite,
                            chickenScreenX + scaledSize, chickenScreenY, chickenScreenX, chickenScreenY + scaledSize,
                            srcX, srcY, srcX + frameWidth, srcY + frameHeight,
                            null);
                } else {
                    // วาดปกติ (หันขวา)
                    g2d.drawImage(chickenSprite,
                            chickenScreenX, chickenScreenY, chickenScreenX + scaledSize, chickenScreenY + scaledSize,
                            srcX, srcY, srcX + frameWidth, srcY + frameHeight,
                            null);
                }
            } else {
                // วาดไก่แบบ fallback
                g2d.setColor(new Color(255, 200, 0, 255)); // สีเหลือง
                g2d.fillOval(chickenScreenX, chickenScreenY, scaledSize, scaledSize);
                
                g2d.setColor(new Color(255, 100, 0, 255)); // สีส้ม
                g2d.fillOval(chickenScreenX + 2, chickenScreenY + 2, scaledSize - 4, scaledSize - 4);
            }
            
            // วาดเงาของไก่
            g2d.setColor(new Color(0, 0, 0, 40));
            int shadowWidth = scaledSize / 2;
            int shadowHeight = 3;
            int shadowX = chickenScreenX + (scaledSize - shadowWidth) / 2;
            int shadowY = chickenScreenY + scaledSize - 1;
            g2d.fillOval(shadowX, shadowY, shadowWidth, shadowHeight);
            
            // วาดชื่อไก่
            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            String chickenName = "Chicken";
            int nameWidth = g2d.getFontMetrics().stringWidth(chickenName);
            int nameX = chickenScreenX + (scaledSize - nameWidth) / 2;
            int nameY = chickenScreenY - 5;
            
            // วาดพื้นหลังสำหรับชื่อ
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillRect(nameX - 2, nameY - 12, nameWidth + 4, 12);
            
            // วาดชื่อไก่
            g2d.setColor(new Color(255, 255, 255, 255));
            g2d.drawString(chickenName, nameX, nameY);
            
            // วาดเส้น debug ระยะห่างระหว่างผู้เล่นกับไก่
            int playerCenterX = mainPlayerScreenX;
            int playerCenterY = mainPlayerScreenY;
            int chickenCenterX = chickenScreenX + scaledSize / 2;
            int chickenCenterY = chickenScreenY + scaledSize / 2;
            
            // คำนวณระยะห่างแบบ top-down
            int realDistance = GameConfig.calculateTopDownDistance(
                chicken.x + GameConfig.CHICKEN_SIZE/2, chicken.y + GameConfig.CHICKEN_SIZE/2,
                mainPlayer.x + GameConfig.PLAYER_SIZE/2, mainPlayer.y + GameConfig.PLAYER_SIZE/2
            );
            
            // วาดเส้นเชื่อมระหว่างผู้เล่นกับไก่
            g2d.setColor(new Color(255, 255, 0, 150)); // สีเหลืองโปร่งใส
            g2d.setStroke(new java.awt.BasicStroke(2));
            g2d.drawLine(playerCenterX, playerCenterY, chickenCenterX, chickenCenterY);
            
            // แสดงระยะห่าง
            int midX = (playerCenterX + chickenCenterX) / 2;
            int midY = (playerCenterY + chickenCenterY) / 2;
            g2d.setColor(new Color(255, 255, 255, 255));
            g2d.drawString("D: " + realDistance, midX, midY);
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

            // วาดเงาของผู้เล่น
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

            // วาดชื่อผู้เล่นเหนือหัว
            if (g instanceof Graphics2D g2d) {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            }
            
            g.setColor(new Color(255, 255, 255, 255));
            g.setFont(new Font("Arial", Font.BOLD, 12));
            String playerName = p.id;
            int nameWidth = g.getFontMetrics().stringWidth(playerName);
            int nameX = screenX + (scaledSize - nameWidth) / 2;
            int nameY = screenY - 8;
            
            // วาดพื้นหลังสำหรับชื่อ
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(nameX - 2, nameY - 12, nameWidth + 4, 14);
            
            // วาดชื่อผู้เล่น
            g.setColor(new Color(255, 255, 255, 255));
            g.drawString(playerName, nameX, nameY);

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
            
            // วาดชื่อผู้เล่นเหนือหัว
            if (g instanceof Graphics2D g2d) {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            }
            
            g.setColor(new Color(255, 255, 255, 255));
            g.setFont(new Font("Arial", Font.BOLD, 12));
            String playerName = p.id;
            int nameWidth = g.getFontMetrics().stringWidth(playerName);
            int nameX = screenX + (scaledSize - nameWidth) / 2;
            int nameY = screenY - 8;
            
            // วาดพื้นหลังสำหรับชื่อ
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(nameX - 2, nameY - 12, nameWidth + 4, 14);
            
            // วาดชื่อผู้เล่น
            g.setColor(new Color(255, 255, 255, 255));
            g.drawString(playerName, nameX, nameY);
        }
    }

    public boolean checkPlayerCollision(Player player, int newX, int newY) {
        synchronized (players) {
            for (Player otherPlayer : players.values()) {
                if (!otherPlayer.id.equals(player.id)) {
                    // ตรวจสอบการชนกันระหว่างผู้เล่น
                    int minDistance = 7 ; // ระยะห่างขั้นต่ำ 4 pixels
                    
                    int distance = GameConfig.calculateTopDownDistance(newX, newY, otherPlayer.x, otherPlayer.y);
                    
                    if (distance < minDistance) {
                        return true; // มีการชนกัน
                    }
                }
            }
        }
        
        // ไม่เช็คการชนกันกับไก่ - ให้ผู้เล่นเดินผ่านไก่ได้

        return false; // ไม่มีการชนกัน
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
    
    // KeyListener methods
    @Override
    public void keyPressed(KeyEvent e) {
        System.out.println("Key pressed: " + e.getKeyCode() + " (TAB = " + KeyEvent.VK_TAB + ")");
        if (e.getKeyCode() == KeyEvent.VK_TAB) {
            System.out.println("TAB pressed - toggling inventory");
            inventoryGUI.toggleVisibility();
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        // ไม่ต้องทำอะไร
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // ไม่ต้องทำอะไร
    }
    
    // Methods สำหรับจัดการคลังเก็บของ
    public void addToInventory(String itemName, int quantity) {
        inventoryGUI.addItem(itemName, quantity);
    }
    
    public void removeFromInventory(String itemName, int quantity) {
        inventoryGUI.removeItem(itemName, quantity);
    }
    
    public int getInventoryQuantity(String itemName) {
        return inventoryGUI.getItemQuantity(itemName);
    }
    
    // Methods สำหรับจัดการฐานข้อมูล
    public void setCurrentPlayerName(String playerName) {
        this.currentPlayerName = playerName;
        loadPlayerData();
    }
    
    private void loadPlayerData() {
        if (currentPlayerName != null && database != null) {
            JSONDatabase.PlayerData playerData = database.getPlayerData(currentPlayerName);
            
            // โหลดข้อมูลเงิน
            Player mainPlayer = players.get(playerId);
            if (mainPlayer != null) {
                mainPlayer.money = playerData.money;
            }
            
            // โหลดข้อมูลคลังเก็บของ
            inventoryGUI.loadItems(playerData.inventory);
            
            System.out.println("Loaded player data for: " + currentPlayerName);
            System.out.println("Money: " + playerData.money);
            System.out.println("Inventory: " + playerData.inventory);
        }
    }
    
    public void savePlayerData() {
        if (currentPlayerName != null && database != null) {
            Player mainPlayer = players.get(playerId);
            if (mainPlayer != null) {
                JSONDatabase.PlayerData playerData = new JSONDatabase.PlayerData();
                playerData.playerName = currentPlayerName;
                playerData.money = mainPlayer.money;
                playerData.level = 1; // ยังไม่ได้ทำระบบ level
                playerData.experience = 0; // ยังไม่ได้ทำระบบ experience
                
                // บันทึกข้อมูลคลังเก็บของ
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
                    
                    // ส่งข้อมูลไก่ที่เกิดใหม่ไปยังเซิร์ฟเวอร์
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
            
            // หาไก่ที่มีตำแหน่งใกล้เคียงกัน
            for (Chicken localChicken : chickens) {
                int distance = GameConfig.calculateTopDownDistance(localChicken.x, localChicken.y, serverChicken.x, serverChicken.y);
                
                if (distance < GameConfig.CHICKEN_SIZE) {
                    // อัปเดตไก่ตัวนี้
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