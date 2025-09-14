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
            
            switch (dir) {
                case "up":    y -= GameConfig.MOVE_SPEED; break;
                case "down":  y += GameConfig.MOVE_SPEED; break;
                case "left":  x -= GameConfig.MOVE_SPEED; break;
                case "right": x += GameConfig.MOVE_SPEED; break;
            }
            
            x = Math.max(0, Math.min(x, GameConfig.WIDTH - GameConfig.PLAYER_SIZE));
            y = Math.max(0, Math.min(y, GameConfig.HEIGHT - GameConfig.PLAYER_SIZE));
        }
    }
    
    public void stop() {
        if (!state.equals("attack2")) {
            this.state = "idle";
        }
    }
    
    public void attack() {
        if (canAttack) {
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
            
            if (elapsed > GameConfig.ATTACK_DURATION) {
                this.state = "idle";
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