package server;

import common.Chicken;
import common.GameConfig;
import common.Player;
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
    private List<Chicken> globalChickens;

    public ClientHandler(Socket socket, Map<String, Player> players, List<ClientHandler> clients, List<Chicken> globalChickens) throws IOException {
        this.socket = socket;
        this.players = players;
        this.clients = clients;
        this.globalChickens = globalChickens;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void run() {
        try {
            playerId = "P" + socket.getPort();
            Player player = new Player(playerId, GameConfig.MAP_WIDTH / 2 +new Random().nextInt(30) , GameConfig.MAP_HEIGHT / 2 +new Random().nextInt(50));
            players.put(playerId, player);

            out.println("ID:" + playerId);
            sendChickens();
            broadcast();

            String input;
            while ((input = in.readLine()) != null) {
                if (input.startsWith("MOVE:")) {
                    String dir = input.split(":")[1];
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
                } else if (input.startsWith("MONEY:")) {
                    int money = Integer.parseInt(input.split(":")[1]);
                    player.money = money;
                    broadcast();
                } else if (input.startsWith("CHICKEN_ATTACK:")) {
                    
                    String chickenData = input.split(":", 2)[1];
                    handleChickenAttack(chickenData);
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

    private void sendChickens() {
        StringBuilder sb = new StringBuilder();
        for (Chicken chicken : globalChickens) {
            sb.append(chicken.toString()).append(";");
        }
        String msg = "CHICKENS:" + sb.toString();
        send(msg);
    }
    
    private void handleChickenAttack(String chickenData) {
        try {
            Chicken attackedChicken = Chicken.fromString(chickenData);
            
            
            for (Chicken globalChicken : globalChickens) {
                int distance = GameConfig.calculateTopDownDistance(globalChicken.x, globalChicken.y, attackedChicken.x, attackedChicken.y);
                
                if (distance < GameConfig.CHICKEN_SIZE) {
                    
                    globalChicken.health = attackedChicken.health;
                    globalChicken.isAlive = attackedChicken.isAlive;
                    globalChicken.state = attackedChicken.state;
                    globalChicken.hitFrame = attackedChicken.hitFrame;
                    globalChicken.deathTime = attackedChicken.deathTime;
                    globalChicken.lastHitTime = attackedChicken.lastHitTime;
                    
                    System.out.println("Updated global chicken: " + globalChicken.x + ", " + globalChicken.y + " Health: " + globalChicken.health + " Alive: " + globalChicken.isAlive + " State: " + globalChicken.state);
                    
                    
                    broadcastChickenUpdate(globalChicken.toString());
                    
                    
                    broadcastAllChickens();
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Error handling chicken attack: " + e.getMessage());
        }
    }
    
    private void broadcastChickenUpdate(String chickenData) {
        String msg = "CHICKEN_UPDATE:" + chickenData;
        for (ClientHandler c : clients) {
            c.send(msg);
        }
    }
    
    private void broadcastAllChickens() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < globalChickens.size(); i++) {
            if (i > 0) sb.append(";");
            sb.append(globalChickens.get(i).toString());
        }
        String msg = "CHICKENS:" + sb.toString();
        for (ClientHandler c : clients) {
            c.send(msg);
        }
    }
    

    private boolean canMove(Player player, String direction) {
        return true;
    }
}
