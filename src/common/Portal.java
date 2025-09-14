package common;

public class Portal {
    public String id;
    public int x, y;
    public int targetX, targetY;
    public String targetPortalId;
    public int animationFrame = 0;
    public long lastFrameTime = 0;
    public long lastTeleportTime = 0;
    
    public Portal(String id, int x, int y, int targetX, int targetY, String targetPortalId) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetPortalId = targetPortalId;
    }
    
    public void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime > 200) {
            animationFrame = (animationFrame + 1) % 6;
            lastFrameTime = currentTime;
        }
    }
    
    public boolean isPlayerNear(Player player) {
        int distance = (int) Math.sqrt(Math.pow(player.x - x, 2) + Math.pow(player.y - y, 2));
        return distance < GameConfig.PORTAL_TELEPORT_DISTANCE;
    }
    
    public void teleportPlayer(Player player) {
        player.x = targetX;
        player.y = targetY;
    }
    
    @Override
    public String toString() {
        return id + "," + x + "," + y + "," + targetX + "," + targetY + "," + targetPortalId;
    }
    
    public static Portal fromString(String data) {
        String[] parts = data.split(",");
        Portal portal = new Portal(parts[0], 
            Integer.parseInt(parts[1]), 
            Integer.parseInt(parts[2]),
            Integer.parseInt(parts[3]), 
            Integer.parseInt(parts[4]), 
            parts[5]);
        return portal;
    }
}
