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
    public static final int ATTACK_RANGE = 30;

    
    public static final int CHICKEN_SIZE = 12;
    public static final int CHICKEN_COUNT = 5;
    public static final int CHICKEN_ANIMATION_FRAMES = 11;

    public static final int CHICKEN_HIT_FRAMES = 5;
    public static final int CHICKEN_REWARD = 5;
    public static final int CHICKEN_RESPAWN_TIME = 8000; 
    
    
    public static final int RENDER_DISTANCE = 75; 
    public static final int VIEW_BUFFER = 75; 
    public static final boolean DEBUG_DISTANCE = false; 
    
    
    public static int calculateTopDownDistance(int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x1 - x2);
        int dy = Math.abs(y1 - y2);
        
        
        
        
        
        int chebyshevDistance = Math.max(dx, dy);
        double euclideanDistance = Math.sqrt(dx * dx + dy * dy);
        return (int) (0.7 * chebyshevDistance + 0.3 * euclideanDistance);
    }
    
    public static int calculateEuclideanDistance(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

}
