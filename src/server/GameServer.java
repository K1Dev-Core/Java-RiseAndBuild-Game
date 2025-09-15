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
        // สร้างไก่ global
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
        
        // จุดเกิดผู้เล่น (800, 840)
        int spawnCenterX = 800;
        int spawnCenterY = 840;
        int spawnRadius = 100; // รัศมีรอบจุดเกิด
        
        for (int i = 0; i < GameConfig.CHICKEN_COUNT; i++) {
            // สร้างไก่ในรัศมีรอบจุดเกิดผู้เล่น
            int x = spawnCenterX + random.nextInt(spawnRadius * 2) - spawnRadius;
            int y = spawnCenterY + random.nextInt(spawnRadius * 2) - spawnRadius;
            
            // ตรวจสอบขอบเขต
            x = Math.max(0, Math.min(x, GameConfig.MAP_WIDTH - GameConfig.CHICKEN_SIZE));
            y = Math.max(0, Math.min(y, GameConfig.MAP_HEIGHT - GameConfig.CHICKEN_SIZE));
            
            globalChickens.add(new Chicken(x, y));
        }
    }
    
    public static List<Chicken> getGlobalChickens() {
        return globalChickens;
    }
}
