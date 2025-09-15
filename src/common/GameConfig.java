package common;

public class GameConfig {
    public static final int PORT = 12345;
    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;
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

    public static final int MUSHROOM_SIZE = 64; // 64x64 pixels
    public static final int MUSHROOM_DETECTION_RANGE = TILE_SIZE * 3;
    public static final int MUSHROOM_REWARD = 10;
    public static final int MUSHROOM_COUNT = 20;
    public static final int MUSHROOM_RESPAWN_TIME = 10000; // 10 วินาที

    // Mushroom Animation Frames
    public static final int MUSHROOM_IDLE_FRAMES = 7;
    public static final int MUSHROOM_HIT_FRAMES = 5;
    public static final int MUSHROOM_DIE_FRAMES = 16;

}
