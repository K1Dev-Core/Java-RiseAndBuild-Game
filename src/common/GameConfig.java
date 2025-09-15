package common;

public class GameConfig {
    public static final int PORT = 12345;
    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;
    public static final int TILE_SIZE = 64;
    public static final int MAP_WIDTH_TILES = 100;
    public static final int MAP_HEIGHT_TILES = 100;
    public static final int MAP_WIDTH = MAP_WIDTH_TILES * TILE_SIZE;
    public static final int MAP_HEIGHT = MAP_HEIGHT_TILES * TILE_SIZE;
    public static final int PLAYER_SIZE = TILE_SIZE * 2;
    public static final int ANIMATION_FRAMES = 8;
    public static final int ANIMATION_SPEED = 12;
    public static final int MOVE_SPEED = TILE_SIZE / 4;
    public static final int ATTACK_DURATION = 640;
    public static final int ATTACK_COOLDOWN = 200;
    
    public static final int MARKER_SIZE = TILE_SIZE;
    public static final int MARKER_DETECTION_RANGE = TILE_SIZE * 3;
    public static final int MARKER_REWARD = 10;
    public static final int MARKER_COUNT = 20;
}
