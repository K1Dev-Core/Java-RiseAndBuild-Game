package client;

import common.GameConfig;
import common.Player;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

public class GameClient extends JFrame {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String playerId;
    private Map<String, Player> players = new HashMap<>();
    private GamePanel panel;
    private boolean wPressed = false;
    private boolean sPressed = false;
    private boolean aPressed = false;
    private boolean dPressed = false;
    private javax.swing.Timer gameTimer;

    public GameClient() throws IOException {
        setTitle("2D Online Game");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
   /*     setUndecorated(true);
        setResizable(false);*/
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);


        socket = new Socket("localhost", GameConfig.PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        panel = new GamePanel(players, playerId);
        panel.setGameClient(this);
        add(panel);
        panel.setFocusable(true);

        new Thread(this::listen).start();
        setupControls();
        setupGameTimer();

        setVisible(true);
        panel.requestFocus();
    }

    private void setupControls() {
        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W:
                        wPressed = true;
                        break;
                    case KeyEvent.VK_S:
                        sPressed = true;
                        break;
                    case KeyEvent.VK_A:
                        aPressed = true;
                        break;
                    case KeyEvent.VK_D:
                        dPressed = true;
                        break;
                    case KeyEvent.VK_ESCAPE:
                        System.exit(0);
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W:
                        wPressed = false;
                        break;
                    case KeyEvent.VK_S:
                        sPressed = false;
                        break;
                    case KeyEvent.VK_A:
                        aPressed = false;
                        break;
                    case KeyEvent.VK_D:
                        dPressed = false;
                        break;
                }
            }
        };

        MouseListener mouseListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                out.println("ATTACK");
                panel.showAttackFeedback();
                panel.requestFocus();
            }
        };

        panel.addKeyListener(keyListener);
        addKeyListener(keyListener);
        panel.addMouseListener(mouseListener);
    }

    private void setupGameTimer() {
        gameTimer = new javax.swing.Timer(16, _ -> {
            if (panel.isDisplayable() && panel.isShowing()) {
                panel.updateGame();
                handleMovement();
                panel.repaint(); // ใช้ repaint() โดยตรงเพื่อลดการกระพริบ
            }
        });
        gameTimer.start();
    }

    private void handleMovement() {
        String direction = "";

        if (wPressed) {
            direction = "up";
        } else if (sPressed) {
            direction = "down";
        } else if (aPressed) {
            direction = "left";
        } else if (dPressed) {
            direction = "right";
        }

        if (!direction.isEmpty()) {
            if (canMoveInDirection(direction)) {
                out.println("MOVE:" + direction);
                panel.checkPlayerMovement();
            }
        } else {
            out.println("STOP");
        }

    }

    public void sendMoneyUpdate(int money) {
        out.println("MONEY:" + money);
    }
    
    public void sendChickenAttack(String chickenData) {
        out.println("CHICKEN_ATTACK:" + chickenData);
    }

    private boolean canMoveInDirection(String direction) {
        Player mainPlayer = panel.getPlayers().get(playerId);
        if (mainPlayer == null)
            return true;

        int newX = mainPlayer.x;
        int newY = mainPlayer.y;

        switch (direction) {
            case "up":
                newY -= GameConfig.MOVE_SPEED;
                break;
            case "down":
                newY += GameConfig.MOVE_SPEED;
                break;
            case "left":
                newX -= GameConfig.MOVE_SPEED;
                break;
            case "right":
                newX += GameConfig.MOVE_SPEED;
                break;
        }

        return !panel.checkPlayerCollision(mainPlayer, newX, newY);
    }

    private void listen() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("ID:")) {
                    playerId = line.substring(3);
                    panel.setPlayerId(playerId);
                    panel.requestFocus();
                } else if (line.startsWith("PLAYERS:")) {
                    updatePlayers(line.substring(8));
                } else if (line.startsWith("CHICKENS:")) {
                    updateChickensFromServer(line.substring(9));
                } else if (line.startsWith("CHICKEN_UPDATE:")) {
                    updateChickenFromServer(line.substring(15));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updatePlayers(String data) {
        synchronized (players) {
            players.clear();
            String[] all = data.split(";");
            for (String p : all) {
                if (!p.isEmpty()) {
                    Player pl = Player.fromString(p);
                    players.put(pl.id, pl);
                }
            }
        }
    }
    
    private void updateChickensFromServer(String chickensData) {
        // อัปเดตไก่ทั้งหมดจากข้อมูลที่ได้รับจากเซิร์ฟเวอร์
        panel.updateChickensFromServer(chickensData);
    }
    
    private void updateChickenFromServer(String chickenData) {
        // อัปเดตไก่จากข้อมูลที่ได้รับจากเซิร์ฟเวอร์
        panel.updateChickenFromServer(chickenData);
    }
    

    public GamePanel getGamePanel() {
        return panel;
    }
    
    public static void main(String[] args) throws Exception {
        new GameClient();
    }
}