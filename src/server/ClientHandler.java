package server;

import common.Player;
import common.GameConfig;
import common.GameMap;
import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String playerId;
    private Map<String, Player> players;
    private List<ClientHandler> clients;
    private static GameMap gameMap;

    static {
        // โหลดแผนที่ครั้งเดียวสำหรับทุก client
        try {
            gameMap = new GameMap("assets/map/map.json", "assets/map/spritesheet.png");
        } catch (Exception e) {
            System.err.println("ไม่สามารถโหลดแผนที่ได้: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public ClientHandler(Socket socket, Map<String, Player> players, List<ClientHandler> clients) throws IOException {
        this.socket = socket;
        this.players = players;
        this.clients = clients;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void run() {
        try {
            playerId = "P" + socket.getPort();
            Player player = new Player(playerId, 200, 200);
            players.put(playerId, player);

            out.println("ID:" + playerId);
            broadcast();

            String input;
            while ((input = in.readLine()) != null) {
                if (input.startsWith("MOVE:")) {
                    String dir = input.split(":")[1];
                    // ตรวจสอบ collision ก่อนเคลื่อนที่
                    if (canMove(player, dir)) {
                        player.move(dir);
                        broadcast();
                    }
                } else if (input.equals("STOP")) {
                    player.stop();
                    broadcast();
                } else if (input.equals("ATTACK")) {
                    player.attack();
                    broadcast();
                    broadcast();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            players.remove(playerId);
            clients.remove(this);
            broadcast();
        }
    }

    void send(String msg) {
        out.println(msg);
    }

    private void broadcast() {
        for (Player p : players.values()) {
            p.updateState();
        }

        StringBuilder sb = new StringBuilder();
        for (Player p : players.values()) {
            sb.append(p.toString()).append(";");
        }
        String msg = "PLAYERS:" + sb.toString();

        for (ClientHandler c : clients) {
            c.send(msg);
        }
    }

    private boolean canMove(Player player, String direction) {
        if (gameMap == null)
            return true; // ถ้าไม่มีแผนที่ ให้เคลื่อนที่ได้

        int newX = player.x;
        int newY = player.y;
        int moveSpeed = GameConfig.MOVE_SPEED;

        // คำนวณตำแหน่งใหม่
        switch (direction) {
            case "up":
                newY -= moveSpeed;
                break;
            case "down":
                newY += moveSpeed;
                break;
            case "left":
                newX -= moveSpeed;
                break;
            case "right":
                newX += moveSpeed;
                break;
        }

        // ตรวจสอบขอบเขตแผนที่
        int mapWidthPixels = gameMap.getMapWidthPixels();
        int mapHeightPixels = gameMap.getMapHeightPixels();
        int playerSize = GameConfig.PLAYER_SIZE;

        if (newX < 0 || newY < 0 ||
                newX + playerSize > mapWidthPixels ||
                newY + playerSize > mapHeightPixels) {
            return false;
        }

        // ตรวจสอบ collision กับ tiles
        int[] checkPoints = {
                newX, newY, // มุมซ้ายบน
                newX + playerSize, newY, // มุมขวาบน
                newX, newY + playerSize, // มุมซ้ายล่าง
                newX + playerSize, newY + playerSize // มุมขวาล่าง
        };

        for (int i = 0; i < checkPoints.length; i += 2) {
            int checkX = checkPoints[i];
            int checkY = checkPoints[i + 1];

            if (gameMap.hasCollisionAt(checkX, checkY)) {
                return false;
            }
        }

        return true;
    }
}