package common;

import java.awt.*;

public class BlacksmithShop {
    public int x, y;
    public int width, height;
    public boolean isActive;
    public Image sprite;
    public int currentFrame;
    public long lastFrameTime;
    public int frameCount = 6; // 6 คอลัมน์
    public int frameWidth, frameHeight;
    
    public BlacksmithShop(int x, int y) {
        this.x = x;
        this.y = y;
        this.width = 24; // ขนาดร้านค้า (เล็กลง)
        this.height = 24;
        this.isActive = false;
        this.currentFrame = 0;
        this.lastFrameTime = System.currentTimeMillis();
    }
    
    public void setSprite(Image sprite) {
        this.sprite = sprite;
        if (sprite != null) {
            this.frameWidth = sprite.getWidth(null) / frameCount;
            this.frameHeight = sprite.getHeight(null);
        }
    }
    
    public void update() {
        // อัปเดตแอนิเมชัน
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime > 200) { // เปลี่ยนเฟรมทุก 200ms
            currentFrame = (currentFrame + 1) % frameCount;
            lastFrameTime = currentTime;
        }
    }
    
    public void draw(Graphics2D g2d, int screenX, int screenY, double zoom) {
        if (sprite == null) return;
        
        int scaledWidth = (int) (width * zoom);
        int scaledHeight = (int) (height * zoom);
        
        // วาดเฟรมปัจจุบัน
        int srcX = currentFrame * frameWidth;
        int srcY = 0;
        
        g2d.drawImage(sprite, 
            screenX, screenY, screenX + scaledWidth, screenY + scaledHeight,
            srcX, srcY, srcX + frameWidth, srcY + frameHeight, null);
        
        // วาดชื่อร้านค้า
        g2d.setColor(new Color(255, 215, 0)); // สีทอง
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        String shopName = "Blacksmith Shop";
        int nameX = screenX + (scaledWidth - fm.stringWidth(shopName)) / 2;
        int nameY = screenY - 5;
        g2d.drawString(shopName, nameX, nameY);
        
        // วาดข้อความโต้ตอบถ้าใกล้
        if (isActive) {
            g2d.setColor(new Color(100, 255, 100));
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            String interactText = "Press SPACE to open shop";
            int textX = screenX + (scaledWidth - g2d.getFontMetrics().stringWidth(interactText)) / 2;
            int textY = screenY + scaledHeight + 15;
            g2d.drawString(interactText, textX, textY);
        }
    }
    
    public boolean isPlayerNearby(Player player, int interactionRange) {
        int playerCenterX = player.x + GameConfig.PLAYER_SIZE / 2;
        int playerCenterY = player.y + GameConfig.PLAYER_SIZE / 2;
        int shopCenterX = x + width / 2;
        int shopCenterY = y + height / 2;
        
        int distance = GameConfig.calculateTopDownDistance(
            playerCenterX, playerCenterY, shopCenterX, shopCenterY
        );
        
        return distance <= interactionRange;
    }
}
