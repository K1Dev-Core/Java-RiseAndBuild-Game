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
    public int animationSpeed = 8; 
    public int animationFrames = 11;
    public String state = "idle"; 
    public long deathTime = 0;
    public int hitFrame = 0;
    public long lastHitTime = 0;
    public int originalX, originalY; 
    
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
        
        update(new java.util.ArrayList<>());
    }
    
    public void update(java.util.List<Chicken> allChickens) {
        long currentTime = System.currentTimeMillis();
        
        if (state.equals("dead")) {
            
            if (currentTime - deathTime > GameConfig.CHICKEN_RESPAWN_TIME) {
                respawn();
            }
            return;
        }
        
        if (!isAlive) return;
        
        
        if (currentTime - lastAnimationTime > (1000 / animationSpeed)) {
            if (state.equals("hit")) {
                hitFrame++;
                System.out.println("Chicken hit animation frame: " + hitFrame + "/" + GameConfig.CHICKEN_HIT_FRAMES);
                if (hitFrame >= GameConfig.CHICKEN_HIT_FRAMES) {
                    state = "idle";
                    hitFrame = 0;
                    System.out.println("Chicken hit animation finished, returning to idle");
                }
            } else {
                animationFrame = (animationFrame + 1) % animationFrames;
            }
            lastAnimationTime = currentTime;
        }
        
        
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > GameConfig.MAP_WIDTH - GameConfig.CHICKEN_SIZE) {
            x = GameConfig.MAP_WIDTH - GameConfig.CHICKEN_SIZE;
        }
        if (y > GameConfig.MAP_HEIGHT - GameConfig.CHICKEN_SIZE) {
            y = GameConfig.MAP_HEIGHT - GameConfig.CHICKEN_SIZE;
        }
        
        
        checkChickenCollision(allChickens);
    }
    
    private void checkChickenCollision(java.util.List<Chicken> allChickens) {
        for (Chicken otherChicken : allChickens) {
            if (otherChicken != this && otherChicken.isAlive && !otherChicken.state.equals("dead")) {
                int minDistance = GameConfig.CHICKEN_SIZE + 2; 
                
                int distance = GameConfig.calculateTopDownDistance(x, y, otherChicken.x, otherChicken.y);
                
                if (distance < minDistance) {
                    
                    if (distance > 0) {
                        int dx = x - otherChicken.x;
                        int dy = y - otherChicken.y;
                        double pushX = (dx / (double) distance) * (minDistance - distance) / 2;
                        double pushY = (dy / (double) distance) * (minDistance - distance) / 2;
                        
                        int newX = (int) (x + pushX);
                        int newY = (int) (y + pushY);
                        
                        
                        if (newX >= 0 && newX <= GameConfig.MAP_WIDTH - GameConfig.CHICKEN_SIZE) {
                            x = newX;
                        }
                        if (newY >= 0 && newY <= GameConfig.MAP_HEIGHT - GameConfig.CHICKEN_SIZE) {
                            y = newY;
                        }
                    }
                }
            }
        }
    }
    

    public void takeDamage(int damage) {
        if (!isAlive || state.equals("dead")) {
            System.out.println("Chicken cannot be attacked - not alive or already dead");
            return;
        }
        
        System.out.println("Chicken taking damage: " + damage + " Health before: " + health);
        health -= damage;
        state = "hit";
        hitFrame = 0;
        lastHitTime = System.currentTimeMillis();
        lastAnimationTime = System.currentTimeMillis(); 
        
        if (health <= 0) {
            isAlive = false;
            state = "dead";
            deathTime = System.currentTimeMillis();
            System.out.println("Chicken died!");
        } else {
            System.out.println("Chicken health after damage: " + health);
        }
    }
    
    public void respawn() {
        isAlive = true;
        health = maxHealth;
        state = "idle";
        x = originalX; 
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
        return "Chicken," + x + "," + y + "," + direction + "," + animationFrame + "," + isAlive + "," + health + "," + state + "," + deathTime;
    }
    
    public static Chicken fromString(String data) {
        String[] parts = data.split(",");
        Chicken chicken = new Chicken(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        chicken.direction = parts[3];
        chicken.animationFrame = Integer.parseInt(parts[4]);
        chicken.isAlive = Boolean.parseBoolean(parts[5]);
        chicken.health = Integer.parseInt(parts[6]);
        if (parts.length > 7) {
            chicken.state = parts[7];
        }
        if (parts.length > 8) {
            chicken.deathTime = Long.parseLong(parts[8]);
        }
        return chicken;
    }
}
