package server;

import common.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final Map<String, Player> players = new HashMap<>();
    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final List<Chicken> globalChickens = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        
        createGlobalChickens();
        
        try (ServerSocket serverSocket = new ServerSocket(GameConfig.PORT)) {
            System.out.println("Server started on " + GameConfig.PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, players, clients, globalChickens);
                clients.add(handler);
                new Thread(handler).start();
            }
        }
    }
    
    private static void createGlobalChickens() {
        globalChickens.clear();
        Random random = new Random();
        
        int spawnCenterX = GameConfig.MAP_WIDTH / 2;
        int spawnCenterY = GameConfig.MAP_HEIGHT / 2;
        int spawnRadius = 300; 
        
        for (int i = 0; i < GameConfig.CHICKEN_COUNT; i++) {
            int x = spawnCenterX + random.nextInt(spawnRadius * 2) - spawnRadius;
            int y = spawnCenterY + random.nextInt(spawnRadius * 2) - spawnRadius;
            
            x = Math.max(50, Math.min(x, GameConfig.MAP_WIDTH - GameConfig.CHICKEN_SIZE - 50));
            y = Math.max(50, Math.min(y, GameConfig.MAP_HEIGHT - GameConfig.CHICKEN_SIZE - 50));
            
            globalChickens.add(new Chicken(x, y));
            System.out.println("Created chicken at: " + x + ", " + y);
        }
        System.out.println("Total chickens created: " + globalChickens.size());
    }
    
    public static List<Chicken> getGlobalChickens() {
        return globalChickens;
    }
}
