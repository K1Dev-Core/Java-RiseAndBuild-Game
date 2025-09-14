package client;

import common.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;



public class GameClient extends JFrame {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String playerId;
    private Map<String, Player> players = new HashMap<>();
    private GamePanel panel;

    public GameClient() throws IOException {
        setTitle("2D Online Game");
        setSize(GameConfig.WIDTH, GameConfig.HEIGHT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        socket = new Socket("localhost", GameConfig.PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        panel = new GamePanel(players, playerId);
        add(panel);
        panel.setFocusable(true);
        panel.requestFocus();

        new Thread(this::listen).start();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W: out.println("MOVE:up"); break;
                    case KeyEvent.VK_S: out.println("MOVE:down"); break;
                    case KeyEvent.VK_A: out.println("MOVE:left"); break;
                    case KeyEvent.VK_D: out.println("MOVE:right"); break;
                    case KeyEvent.VK_SPACE: out.println("ATTACK"); break;
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W:
                    case KeyEvent.VK_S:
                    case KeyEvent.VK_A:
                    case KeyEvent.VK_D:
                        out.println("STOP");
                        break;
                }
            }
        });
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                out.println("ATTACK");
            }
        });

        setVisible(true);

        new Timer(30, e -> panel.repaint()).start();
    }

    private void listen() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("ID:")) {
                    playerId = line.substring(3);
                    panel.setPlayerId(playerId);
                    panel.repaint();
                } else if (line.startsWith("PLAYERS:")) {
                    updatePlayers(line.substring(8));
                    panel.repaint();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updatePlayers(String data) {
        players.clear();
        String[] all = data.split(";");
        for (String p : all) {
            if (!p.isEmpty()) {
                Player pl = Player.fromString(p);
                players.put(pl.id, pl);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new GameClient();
    }
}
