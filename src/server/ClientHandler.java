package server;

import common.Player;
import common.Portal;
import common.GameConfig;
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
    private Map<String, Portal> portals;

    public ClientHandler(Socket socket, Map<String, Player> players, List<ClientHandler> clients) throws IOException {
        this.socket = socket;
        this.players = players;
        this.clients = clients;
        this.portals = new HashMap<>();
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        createPortals();
    }

    @Override
    public void run() {
        try {
            playerId = "P" + socket.getPort();
            Player player = new Player(playerId, 200, 200);
            players.put(playerId, player);

            out.println("ID:" + playerId);
            broadcast();
            broadcastPortals();

            String input;
            while ((input = in.readLine()) != null) {
                if (input.startsWith("MOVE:")) {
                    String dir = input.split(":")[1];
                    player.move(dir);
                    broadcast();
                } else if (input.equals("STOP")) {
                    player.stop();
                    broadcast();
                } else if (input.equals("ATTACK")) {
                    player.attack();
                    broadcast();
                } else if (input.equals("TELEPORT")) {
                    handleTeleport(player);
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
    
    private void createPortals() {
        for (int i = 0; i < GameConfig.PORTAL_POSITIONS.length; i++) {
            int[] pos = GameConfig.PORTAL_POSITIONS[i];
            String portalId = "Portal" + (char)('A' + i);
            String targetId = "Portal" + (char)('A' + (i == 0 ? 1 : 0));
            
            Portal portal = new Portal(portalId, pos[0], pos[1], pos[2], pos[3], targetId);
            portals.put(portalId, portal);
        }
    }
    
    private void broadcastPortals() {
        StringBuilder sb = new StringBuilder();
        for (Portal p : portals.values()) {
            sb.append(p.toString()).append(";");
        }
        String msg = "PORTALS:" + sb.toString();
        
        for (ClientHandler c : clients) {
            c.send(msg);
        }
    }
    
    private void handleTeleport(Player player) {
        for (Portal portal : portals.values()) {
            if (portal.isPlayerInside(player) && portal.canTeleport()) {
                portal.teleportPlayer(player);
                System.out.println("Server: Teleported player " + player.id + " to " + player.x + ", " + player.y);
                break;
            }
        }
    }
}