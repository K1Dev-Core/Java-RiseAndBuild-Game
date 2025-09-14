package client;

import common.*;
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
    private boolean spacePressed = false;
    private javax.swing.Timer gameTimer;

    public GameClient() throws IOException {
        setTitle("2D Online Game");
        setSize(GameConfig.WIDTH, GameConfig.HEIGHT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        socket = new Socket("localhost", GameConfig.PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        panel = new GamePanel(players, playerId);
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
                    case KeyEvent.VK_W: wPressed = true; break;
                    case KeyEvent.VK_S: sPressed = true; break;
                    case KeyEvent.VK_A: aPressed = true; break;
                    case KeyEvent.VK_D: dPressed = true; break;
                    case KeyEvent.VK_SPACE: 
                        if (!spacePressed) {
                            spacePressed = true;
                        }
                        break;
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W: wPressed = false; break;
                    case KeyEvent.VK_S: sPressed = false; break;
                    case KeyEvent.VK_A: aPressed = false; break;
                    case KeyEvent.VK_D: dPressed = false; break;
                    case KeyEvent.VK_SPACE: spacePressed = false; break;
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
        gameTimer = new javax.swing.Timer(16, e -> {
            handleMovement();
            panel.repaint();
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
            out.println("MOVE:" + direction);
        } else {
            out.println("STOP");
        }
        
        if (spacePressed) {
            out.println("ATTACK");
            panel.showAttackFeedback();
            spacePressed = false;
        }
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
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updatePlayers(String data) {
        synchronized(players) {
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

    public static void main(String[] args) throws Exception {
        new GameClient();
    }
}