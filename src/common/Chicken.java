package common;

import java.util.Random;

public class Chicken {
    public int x, y;
    public String direction = "right";
    public int animationFrame = 0;
    public long lastMoveTime = 0;
    public long lastAnimationTime = 0;
    public boolean isAlive = true;
    public int health = 3;
    public int maxHealth = 3;
    public long lastDirectionChange = 0;
    public int moveSpeed = 1;
    public int animationSpeed = 8; // เฟรมต่อวินาที
    public int animationFrames = 11;
    public String state = "idle"; // idle, hit, dead
    public long deathTime = 0;
    public int hitFrame = 0;
    public long lastHitTime = 0;
    public int originalX, originalY; // เก็บตำแหน่งเดิม
    
    private Random random = new Random();
    
    public Chicken(int x, int y) {
        this.x = x;
        this.y = y;
        this.originalX = x;
        this.originalY = y;
        this.lastMoveTime = System.currentTimeMillis();
        this.lastAnimationTime = System.currentTimeMillis();
        this.lastDirectionChange = System.currentTimeMillis();
    }
    
    public void update() {
        long currentTime = System.currentTimeMillis();
        
        if (state.equals("dead")) {
            // ตรวจสอบการเกิดใหม่
            if (currentTime - deathTime > GameConfig.CHICKEN_RESPAWN_TIME) {
                respawn();
            }
            return;
        }
        
        if (!isAlive) return;
        
        // อัปเดต animation ตาม state
        if (currentTime - lastAnimationTime > (1000 / animationSpeed)) {
            if (state.equals("hit")) {
                hitFrame++;
                if (hitFrame >= GameConfig.CHICKEN_HIT_FRAMES) {
                    state = "idle";
                    hitFrame = 0;
                }
            } else {
                animationFrame = (animationFrame + 1) % animationFrames;
            }
            lastAnimationTime = currentTime;
        }
        
        // ตรวจสอบขอบเขตแผนที่ - ป้องกันไม่ให้ไก่อยู่นอกขอบเขต
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > GameConfig.MAP_WIDTH - GameConfig.CHICKEN_SIZE) {
            x = GameConfig.MAP_WIDTH - GameConfig.CHICKEN_SIZE;
        }
        if (y > GameConfig.MAP_HEIGHT - GameConfig.CHICKEN_SIZE) {
            y = GameConfig.MAP_HEIGHT - GameConfig.CHICKEN_SIZE;
        }
    }
    

    public void takeDamage(int damage) {
        if (!isAlive || state.equals("dead")) return;
        
        health -= damage;
        state = "hit";
        hitFrame = 0;
        lastHitTime = System.currentTimeMillis();
        
        if (health <= 0) {
            isAlive = false;
            state = "dead";
            deathTime = System.currentTimeMillis();
        }
    }
    
    public void respawn() {
        isAlive = true;
        health = maxHealth;
        state = "idle";
        x = originalX; // เกิดใหม่ที่จุดเดิม
        y = originalY;
        lastMoveTime = System.currentTimeMillis();
        lastAnimationTime = System.currentTimeMillis();
        lastDirectionChange = System.currentTimeMillis();
        animationFrame = 0;
        hitFrame = 0;
    }
    
    public boolean canBeAttacked() {
        return isAlive && !state.equals("dead") && !state.equals("hit");
    }
    
    public int getReward() {
        return GameConfig.CHICKEN_REWARD;
    }
    
    @Override
    public String toString() {
        return "Chicken," + x + "," + y + "," + direction + "," + animationFrame + "," + isAlive + "," + health;
    }
    
    public static Chicken fromString(String data) {
        String[] parts = data.split(",");
        Chicken chicken = new Chicken(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        chicken.direction = parts[3];
        chicken.animationFrame = Integer.parseInt(parts[4]);
        chicken.isAlive = Boolean.parseBoolean(parts[5]);
        chicken.health = Integer.parseInt(parts[6]);
        return chicken;
    }
}
