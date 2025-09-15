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
    public static final int ATTACK_RANGE = 35;

    // Chicken Settings
    public static final int CHICKEN_SIZE = 12;
    public static final int CHICKEN_COUNT = 3;
    public static final int CHICKEN_ANIMATION_FRAMES = 11;

    public static final int CHICKEN_HIT_FRAMES = 5;
    public static final int CHICKEN_REWARD = 5;
    public static final int CHICKEN_RESPAWN_TIME = 8000; // 10 วินาที
    
    // Distance calculation methods
    public static int calculateTopDownDistance(int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x1 - x2);
        int dy = Math.abs(y1 - y2);
        // ใช้ Chebyshev distance (King's move) ที่เหมาะสมกับเกมแนว top-down
        return Math.max(dx, dy);
    }
    
    public static int calculateEuclideanDistance(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

}
