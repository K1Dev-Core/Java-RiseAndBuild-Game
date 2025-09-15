package client;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JSONDatabase {
    private static final String DATABASE_FILE = "player_data.txt";
    private Map<String, PlayerData> playerDatabase;
    
    public JSONDatabase() {
        playerDatabase = new HashMap<>();
        loadDatabase();
    }
    
    
    public static class PlayerData {
        public String playerName;
        public int money;
        public Map<String, Integer> inventory;
        public int level;
        public int experience;
        
        public PlayerData() {
            this.playerName = "";
            this.money = 0;
            this.inventory = new HashMap<>();
            this.level = 1;
            this.experience = 0;
        }
        
        public PlayerData(String playerName, int money, Map<String, Integer> inventory, int level, int experience) {
            this.playerName = playerName;
            this.money = money;
            this.inventory = inventory != null ? inventory : new HashMap<>();
            this.level = level;
            this.experience = experience;
        }
    }
    
    
    private void loadDatabase() {
        try {
            if (Files.exists(Paths.get(DATABASE_FILE))) {
                String content = new String(Files.readAllBytes(Paths.get(DATABASE_FILE)));
                parseTextDatabase(content);
            }
        } catch (IOException e) {
            System.out.println("Error loading database: " + e.getMessage());
        }
    }
    
    
    public void saveDatabase() {
        try {
            String textContent = generateTextDatabase();
            Files.write(Paths.get(DATABASE_FILE), textContent.getBytes());
            System.out.println("Database saved successfully!");
        } catch (IOException e) {
            System.out.println("Error saving database: " + e.getMessage());
        }
    }
    
    
    private void parseTextDatabase(String content) {
        try {
            System.out.println("Loading text database...");
            String[] lines = content.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue; 
                
                if (line.startsWith("PLAYER:")) {
                    parsePlayerLine(line);
                }
            }
        } catch (Exception e) {
            System.out.println("Error parsing text database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    private void parsePlayerLine(String line) {
        try {
            
            
            
            String data = line.substring(7); 
            String[] parts = data.split("\\|");
            
            if (parts.length >= 4) {
                String playerName = parts[0];
                int money = Integer.parseInt(parts[1]);
                int level = Integer.parseInt(parts[2]);
                int experience = Integer.parseInt(parts[3]);
                
                Map<String, Integer> inventory = new HashMap<>();
                if (parts.length > 4 && !parts[4].isEmpty()) {
                    
                    String[] items = parts[4].split(",");
                    for (String item : items) {
                        if (item.contains(":")) {
                            String[] itemParts = item.split(":");
                            if (itemParts.length == 2) {
                                String itemName = itemParts[0];
                                int quantity = Integer.parseInt(itemParts[1]);
                                inventory.put(itemName, quantity);
                            }
                        }
                    }
                }
                
                PlayerData playerData = new PlayerData(playerName, money, inventory, level, experience);
                playerDatabase.put(playerName, playerData);
                System.out.println("Loaded player: " + playerName + " Money: " + money + " Inventory: " + inventory);
            }
        } catch (Exception e) {
            System.out.println("Error parsing player line: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    private String generateTextDatabase() {
        StringBuilder text = new StringBuilder();
        text.append("# Player Database\n");
        text.append("# Format: PLAYER:playerName|money|level|experience|inventory\n");
        text.append("# Inventory format: item1:quantity1,item2:quantity2\n\n");
        
        for (Map.Entry<String, PlayerData> entry : playerDatabase.entrySet()) {
            String playerName = entry.getKey();
            PlayerData playerData = entry.getValue();
            
            text.append("PLAYER:").append(playerName).append("|");
            text.append(playerData.money).append("|");
            text.append(playerData.level).append("|");
            text.append(playerData.experience).append("|");
            
            
            if (playerData.inventory != null && !playerData.inventory.isEmpty()) {
                boolean first = true;
                for (Map.Entry<String, Integer> item : playerData.inventory.entrySet()) {
                    if (!first) text.append(",");
                    text.append(item.getKey()).append(":").append(item.getValue());
                    first = false;
                }
            }
            text.append("\n");
        }
        
        return text.toString();
    }
    
    
    
    public PlayerData getPlayerData(String playerName) {
        return playerDatabase.getOrDefault(playerName, new PlayerData());
    }
    
    
    public void savePlayerData(String playerName, PlayerData playerData) {
        playerData.playerName = playerName;
        playerDatabase.put(playerName, playerData);
        saveDatabase();
    }
    
    
    public boolean playerExists(String playerName) {
        return playerDatabase.containsKey(playerName);
    }
    
    
    public Set<String> getAllPlayerNames() {
        return playerDatabase.keySet();
    }
    
    
    public void deletePlayer(String playerName) {
        playerDatabase.remove(playerName);
        saveDatabase();
    }
}
