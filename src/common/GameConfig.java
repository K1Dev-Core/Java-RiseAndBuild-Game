package common;

public class GameConfig {
    public static final int PORT = 12345;
    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;
    public static final int MAP_WIDTH = 5000;
    public static final int MAP_HEIGHT = 5000;
    public static final int PLAYER_SIZE = 96;
    public static final int SPRITE_WIDTH = 22;
    public static final int SPRITE_HEIGHT = 34;
    public static final int CANVAS_WIDTH = 96;
    public static final int CANVAS_HEIGHT = 80;
    public static final int ANIMATION_FRAMES = 8;
    public static final int ANIMATION_SPEED = 12;
    public static final int MOVE_SPEED = 4;
    public static final int ATTACK_DURATION = 640;
    public static final int ATTACK_COOLDOWN = 200;
    public static final int PORTAL_SIZE = 256;
    public static final int PORTAL_ANIMATION_FRAMES = 6;
    public static final int PORTAL_TELEPORT_DISTANCE = 128;
    public static final int PORTAL_SAFE_DISTANCE = 128;
    public static final int GRID_SIZE = 64;
    public static final int PORTAL_GRID_SIZE = 4;
    public static final int TELEPORT_GRID_DISTANCE = 2;
    
    public static final int[][] PORTAL_POSITIONS = {
        {200, 268, 3000, 3000},
        {3000, 3000, 200, 268}
    };
}
