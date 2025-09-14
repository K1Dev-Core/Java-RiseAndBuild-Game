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
    
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public GamePanel(Map<String, Player> players, String playerId) {
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
                Image img = new ImageIcon(path).getImage();
                sprites.put(key, img);
            }
        }
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        updateAnimation();
        g.setColor(new Color(50, 50, 50));
        g.fillRect(0, 0, getWidth(), getHeight());
        for (Player p : players.values()) {
            drawPlayer(g, p);
        }
    }
    
    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime > (1000 / GameConfig.ANIMATION_SPEED)) {
            animationFrame = (animationFrame + 1) % GameConfig.ANIMATION_FRAMES;
            lastFrameTime = currentTime;
        }
    }
    
    private void drawPlayer(Graphics g, Player p) {
        p.updateState();
        String key = p.state + "_" + p.direction;
        Image img = sprites.getOrDefault(key, sprites.get("idle_down"));
        
        if (img != null) {
            int frameWidth = img.getWidth(null) / GameConfig.ANIMATION_FRAMES;
            int frameHeight = img.getHeight(null);
            int srcX = animationFrame * frameWidth;
            int srcY = 0;
            
            g.drawImage(img, 
                p.x, p.y, p.x + GameConfig.PLAYER_SIZE, p.y + GameConfig.PLAYER_SIZE,
                srcX, srcY, srcX + frameWidth, srcY + frameHeight,
                null);
                
            if (p.state.equals("attack1")) {
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.RED);
            }
            g.drawRect(p.x, p.y, GameConfig.PLAYER_SIZE, GameConfig.PLAYER_SIZE);
            
            g.setColor(Color.WHITE);
            g.drawString(p.id, p.x, p.y - 5);
        }
    }
}
