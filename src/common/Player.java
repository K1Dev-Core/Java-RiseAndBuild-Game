package common;

public class Player {
    public String id;
    public int x, y;
    public String direction = "down";
    public String state = "idle";
    public long lastAttackTime = 0;
    public boolean canAttack = true;

    public Player(String id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public void move(String dir) {
        if (!state.equals("attack2")) {
            this.state = "run";
            this.direction = dir;
            
            int newX = x;
            int newY = y;
            
            switch (dir) {
                case "up":    newY -= GameConfig.MOVE_SPEED; break;
                case "down":  newY += GameConfig.MOVE_SPEED; break;
                case "left":  newX -= GameConfig.MOVE_SPEED; break;
                case "right": newX += GameConfig.MOVE_SPEED; break;
            }
            
            int maxX = GameConfig.MAP_WIDTH - GameConfig.PLAYER_SIZE;
            int maxY = GameConfig.MAP_HEIGHT - GameConfig.PLAYER_SIZE;
            
            newX = Math.max(0, Math.min(newX, maxX));
            newY = Math.max(0, Math.min(newY, maxY));
            
            x = newX;
            y = newY;
        }
    }
    
    public boolean moveWithCollision(String dir, Object mapLoader) {
        if (!state.equals("attack2")) {
            this.state = "run";
            this.direction = dir;
            
            int newX = x;
            int newY = y;
            
            switch (dir) {
                case "up":    newY -= GameConfig.MOVE_SPEED; break;
                case "down":  newY += GameConfig.MOVE_SPEED; break;
                case "left":  newX -= GameConfig.MOVE_SPEED; break;
                case "right": newX += GameConfig.MOVE_SPEED; break;
            }
            
            // ไม่มีการตรวจสอบ collision - วิ่งได้เรื่อยๆ
            
            x = newX;
            y = newY;
            return true;
        }
        return false;
    }
    
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    
    

    
    public void stop() {
        if (!state.equals("attack2")) {
            this.state = "idle";
        }
    }
    
    public void attack() {
        if (canAttack && !state.equals("attack2")) {
            long currentTime = System.currentTimeMillis();
            this.state = "attack2";
            this.lastAttackTime = currentTime;
            this.canAttack = false;
        }
    }
    
    
    public void updateState() {
        if (state.equals("attack2")) {
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - lastAttackTime;
            
            if (elapsed >= GameConfig.ATTACK_DURATION) {
                this.state = "idle";
            }
        } else if (!canAttack) {
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - lastAttackTime;
            
            if (elapsed >= GameConfig.ATTACK_DURATION + GameConfig.ATTACK_COOLDOWN) {
                this.canAttack = true;
            }
        }
    }

    @Override
    public String toString() {
        return id + "," + x + "," + y + "," + direction + "," + state;
    }

    public static Player fromString(String data) {
        String[] parts = data.split(",");
        Player p = new Player(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        p.direction = parts[3];
        p.state = parts[4];
        return p;
    }
}