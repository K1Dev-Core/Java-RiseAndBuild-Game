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
            
            if (newX > maxX || newY > maxY) {
                System.out.println("Boundary check: x=" + newX + " maxX=" + maxX + " y=" + newY + " maxY=" + maxY);
            }
            
            newX = Math.max(0, Math.min(newX, maxX));
            newY = Math.max(0, Math.min(newY, maxY));
            
            x = newX;
            y = newY;
        }
    }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public boolean canMoveTo(int newX, int newY, int portalX, int portalY, int portalSize) {
        int playerCenterX = newX + GameConfig.PLAYER_SIZE / 2;
        int playerCenterY = newY + GameConfig.PLAYER_SIZE / 2;
        int portalCenterX = portalX + portalSize / 2;
        int portalCenterY = portalY + portalSize / 2;
        
        int distance = (int) Math.sqrt(Math.pow(playerCenterX - portalCenterX, 2) + Math.pow(playerCenterY - portalCenterY, 2));
        return distance >= portalSize / 2 + GameConfig.PLAYER_SIZE / 2 + 10;
    }
    
    private boolean isCollidingWithPortal(int newX, int newY) {
        return false;
    }
    
    public boolean isCollidingWithPortal(int newX, int newY, int portalX, int portalY, int portalSize) {
        int playerCenterX = newX + GameConfig.PLAYER_SIZE / 2;
        int playerCenterY = newY + GameConfig.PLAYER_SIZE / 2;
        int portalCenterX = portalX + portalSize / 2;
        int portalCenterY = portalY + portalSize / 2;
        
        int distance = (int) Math.sqrt(Math.pow(playerCenterX - portalCenterX, 2) + Math.pow(playerCenterY - portalCenterY, 2));
        return distance < portalSize / 2 + GameConfig.PLAYER_SIZE / 2;
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