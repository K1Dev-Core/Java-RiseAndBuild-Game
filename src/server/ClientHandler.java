package server;

import common.Player;
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
            Player player = new Player(playerId, 100, 100);
            players.put(playerId, player);

            out.println("ID:" + playerId);
            broadcast();

            String input;
            while ((input = in.readLine()) != null) {
                if (input.startsWith("MOVE:")) {
                    String dir = input.split(":")[1];
                    player.move(dir);
                    broadcast();
                } else if (input.equals("STOP")) {
                    player.state = "idle";
                    broadcast();
                } else if (input.equals("ATTACK")) {
                    player.attack();
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
        StringBuilder sb = new StringBuilder();
        for (Player p : players.values()) {
            sb.append(p.toString()).append(";");
        }
        String msg = "PLAYERS:" + sb.toString();
        for (ClientHandler c : clients) {
            c.send(msg);
        }
    }
}
