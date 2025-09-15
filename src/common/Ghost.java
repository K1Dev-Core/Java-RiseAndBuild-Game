package common;

public class Ghost {
    public int x, y;
    public int animationFrame;
    public long spawnTime;
    public boolean isVisible;
    public String state;
    
    public Ghost(int x, int y) {
        this.x = x;
        this.y = y;
        this.animationFrame = 0;
        this.spawnTime = System.currentTimeMillis();
        this.isVisible = true;
        this.state = "idle";
    }
    
    public void update() {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - spawnTime > GameConfig.GHOST_DURATION) {
            isVisible = false;
            return;
        }
        
        animationFrame = (int) ((currentTime - spawnTime) / 200) % GameConfig.GHOST_ANIMATION_FRAMES;
    }
    
    public boolean shouldDespawn() {
        return !isVisible;
    }
    
    public String toString() {
        return x + "," + y + "," + animationFrame + "," + spawnTime + "," + isVisible + "," + state;
    }
    
    public static Ghost fromString(String data) {
        String[] parts = data.split(",");
        Ghost ghost = new Ghost(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        ghost.animationFrame = Integer.parseInt(parts[2]);
        ghost.spawnTime = Long.parseLong(parts[3]);
        ghost.isVisible = Boolean.parseBoolean(parts[4]);
        ghost.state = parts[5];
        return ghost;
    }
}
