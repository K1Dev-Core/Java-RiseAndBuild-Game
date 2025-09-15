package common;

public class GameConfig {
    public static final int PORT = 12345;
    public static final int TILE_SIZE = 32;
    public static final int MAP_WIDTH_TILES = 50;
    public static final int MAP_HEIGHT_TILES = 50;
    public static final int MAP_WIDTH = MAP_WIDTH_TILES * TILE_SIZE;
    public static final int MAP_HEIGHT = MAP_HEIGHT_TILES * TILE_SIZE;
    public static final int PLAYER_SIZE = 48;
    public static final int ANIMATION_FRAMES = 8;
    public static final int ANIMATION_SPEED = 12;
    public static final int MOVE_SPEED = 1;
    public static final int ATTACK_DURATION = 640;
    public static final int ATTACK_COOLDOWN = 200;
    public static final int ATTACK_RANGE = 25;

    
    public static final int CHICKEN_SIZE = 12;
    public static final int CHICKEN_COUNT = 7;
    public static final int CHICKEN_ANIMATION_FRAMES = 11;

    public static final int CHICKEN_HIT_FRAMES = 5;
    public static final int CHICKEN_REWARD = 5;
    public static final int CHICKEN_RESPAWN_TIME = 8000; 
    
    
    public static final int RENDER_DISTANCE = 60; 
    public static final int VIEW_BUFFER = 60; 
    public static final boolean DEBUG_DISTANCE = true; 
    
    
    public static int calculateTopDownDistance(int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x1 - x2);
        int dy = Math.abs(y1 - y2);
        
        
        
        
        
        int chebyshevDistance = Math.max(dx, dy);
        double euclideanDistance = Math.sqrt(dx * dx + dy * dy);
        return (int) (0.7 * chebyshevDistance + 0.3 * euclideanDistance);
    }
    
    public static int calculateEuclideanDistance(int x1, int y1, int x2, int y2) {
        double dx = (double)(x1 - x2);
        double dy = (double)(y1 - y2);
        double distance = Math.sqrt(dx * dx + dy * dy);
        return (int) Math.round(distance);
    }
    

    public static double calculatePreciseDistance(int x1, int y1, int x2, int y2) {
        double dx = (double)(x1 - x2);
        double dy = (double)(y1 - y2);
        return Math.sqrt(dx * dx + dy * dy);
    }
    

    public static double calculateTopDownScreenDistance(int playerScreenX, int playerScreenY, 
                                                       int targetScreenX, int targetScreenY, 
                                                       float zoom) {
        double dx = (double)(playerScreenX - targetScreenX);
        double dy = (double)(playerScreenY - targetScreenY);
        double screenDistance = Math.sqrt(dx * dx + dy * dy);
        
    
        return screenDistance / zoom;
    }

}
