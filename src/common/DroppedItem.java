package common;

import java.awt.*;
import javax.swing.*;

public class DroppedItem {
    public int x, y;
    public String itemName;
    public int quantity;
    public long dropTime;
    public boolean isCollected;
    public Image itemImage;
    
    
    public static final int ITEM_SIZE = 4;
    
    public DroppedItem(int x, int y, String itemName, int quantity) {
        this.x = x;
        this.y = y;
        this.itemName = itemName;
        this.quantity = quantity;
        this.dropTime = System.currentTimeMillis();
        this.isCollected = false;
        
        
        loadItemImage();
    }
    
    private void loadItemImage() {
        try {
            String imagePath = "assets/sprites/items/" + itemName + ".png";
            itemImage = new ImageIcon(imagePath).getImage();
        } catch (Exception e) {
            System.out.println("Failed to load item image: " + itemName);
        }
    }
    
    public void draw(Graphics2D g2d, int screenX, int screenY, double zoom) {
        if (isCollected) return;
        
        int scaledSize = (int) (ITEM_SIZE * zoom);
        
        
        if (itemImage != null) {
            g2d.drawImage(itemImage, screenX, screenY, scaledSize, scaledSize, null);
        } else {
            
            g2d.setColor(new Color(100, 100, 100));
            g2d.fillRect(screenX, screenY, scaledSize, scaledSize);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(screenX, screenY, scaledSize, scaledSize);
        }
        
        
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        FontMetrics fm = g2d.getFontMetrics();
        String displayText = itemName + " x" + quantity;
        int textWidth = fm.stringWidth(displayText);
        int textX = screenX + (scaledSize - textWidth) / 2;
        int textY = screenY - 5;
        
        
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(textX - 2, textY - 12, textWidth + 4, 12);
        
        
        g2d.setColor(new Color(255, 255, 255, 255));
        g2d.drawString(displayText, textX, textY);
        

    }
    
    public boolean isPlayerNearby(Player player, int pickupRange) {
        if (isCollected) return false;
        
        int playerCenterX = player.x + GameConfig.PLAYER_SIZE / 3;
        int playerCenterY = player.y + GameConfig.PLAYER_SIZE / 3;
        int itemCenterX = x + ITEM_SIZE / 3;
        int itemCenterY = y + ITEM_SIZE / 3;
        
        int distance = GameConfig.calculateTopDownDistance(
            playerCenterX, playerCenterY, itemCenterX, itemCenterY
        );
        
        return distance <= pickupRange;
    }
    
    public void collect() {
        isCollected = true;
    }
    
    public boolean isExpired() {
        
        return System.currentTimeMillis() - dropTime > 300000;
    }
}
