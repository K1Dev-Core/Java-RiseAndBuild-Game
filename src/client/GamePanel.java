package client;

import common.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;

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
    

    public void showAttackFeedback() {
        showAttackFeedback = true;
        attackFeedbackTime = System.currentTimeMillis();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        updateAnimation();
        updateAttackFeedback();
        
        g.setColor(new Color(40, 40, 40));
        g.fillRect(0, 0, getWidth(), getHeight());
        
        
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
    }
    
    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime > (1000 / GameConfig.ANIMATION_SPEED)) {
            boolean isAttacking = false;
            synchronized(players) {
                for (Player p : players.values()) {
                    if (p.state.equals("attack2")) {
                        isAttacking = true;
                        break;
                    }
                }
            }
            
            if (isAttacking) {
                attackAnimationFrame = (attackAnimationFrame + 1) % GameConfig.ANIMATION_FRAMES;
                animationFrame = attackAnimationFrame;
            } else {
                animationFrame = (animationFrame + 1) % GameConfig.ANIMATION_FRAMES;
                attackAnimationFrame = 0;
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
        p.updateState();
        String key = p.state + "_" + p.direction;
        Image img = sprites.get(key);
        
        if (!p.state.equals(lastPlayerState)) {
            if (p.state.equals("attack2")) {
                attackAnimationFrame = 0;
            }
            lastPlayerState = p.state;
        }
        
        if (img != null) {
            int frameWidth = img.getWidth(null) / GameConfig.ANIMATION_FRAMES;
            int frameHeight = img.getHeight(null);
            
            int currentFrame = p.state.equals("attack2") ? attackAnimationFrame : animationFrame;
            int srcX = currentFrame * frameWidth;
            int srcY = 0;
            
            g.drawImage(img, 
                p.x, p.y, p.x + GameConfig.PLAYER_SIZE, p.y + GameConfig.PLAYER_SIZE,
                srcX, srcY, srcX + frameWidth, srcY + frameHeight,
                null);
                
            if (p.state.equals("attack2")) {
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.RED);
            }
            g.drawRect(p.x, p.y, GameConfig.PLAYER_SIZE, GameConfig.PLAYER_SIZE);
            
            g.setColor(Color.WHITE);
            g.drawString(p.id + " - " + p.state, p.x, p.y - 5);
        } else {
            g.setColor(Color.BLUE);
            g.fillRect(p.x, p.y, GameConfig.PLAYER_SIZE, GameConfig.PLAYER_SIZE);
            g.setColor(Color.WHITE);
            g.drawString(p.id + " - " + p.state, p.x, p.y - 5);
        }
    }
    
}