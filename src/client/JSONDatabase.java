package client;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JSONDatabase {
    private static final String DATABASE_FILE = "player_data.json";
    private Map<String, PlayerData> playerDatabase;
    
    public JSONDatabase() {
        playerDatabase = new HashMap<>();
        loadDatabase();
    }
    
    // Inner class สำหรับเก็บข้อมูลผู้เล่น
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
    
    // โหลดฐานข้อมูลจากไฟล์
    private void loadDatabase() {
        try {
            if (Files.exists(Paths.get(DATABASE_FILE))) {
                String jsonContent = new String(Files.readAllBytes(Paths.get(DATABASE_FILE)));
                parseJSON(jsonContent);
            }
        } catch (IOException e) {
            System.out.println("Error loading database: " + e.getMessage());
        }
    }
    
    // บันทึกฐานข้อมูลลงไฟล์
    public void saveDatabase() {
        try {
            String jsonContent = generateJSON();
            Files.write(Paths.get(DATABASE_FILE), jsonContent.getBytes());
            System.out.println("Database saved successfully!");
        } catch (IOException e) {
            System.out.println("Error saving database: " + e.getMessage());
        }
    }
    
    // แปลง JSON เป็น Map
    private void parseJSON(String json) {
        try {
            // Simple JSON parser (ไม่ใช้ library)
            json = json.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                json = json.substring(1, json.length() - 1);
                
                String[] players = json.split("},");
                for (String playerStr : players) {
                    if (playerStr.endsWith("}")) {
                        playerStr = playerStr.substring(0, playerStr.length() - 1);
                    }
                    playerStr = playerStr.trim();
                    if (playerStr.startsWith("\"") && playerStr.contains(":")) {
                        parsePlayerData(playerStr);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error parsing JSON: " + e.getMessage());
        }
    }
    
    // แปลงข้อมูลผู้เล่นจาก JSON
    private void parsePlayerData(String playerStr) {
        try {
            // หาชื่อผู้เล่น
            int nameStart = playerStr.indexOf("\"") + 1;
            int nameEnd = playerStr.indexOf("\"", nameStart);
            String playerName = playerStr.substring(nameStart, nameEnd);
            
            // หาข้อมูลผู้เล่น
            int dataStart = playerStr.indexOf("{", nameEnd);
            if (dataStart != -1) {
                String dataStr = playerStr.substring(dataStart);
                PlayerData playerData = parsePlayerDataObject(dataStr);
                playerDatabase.put(playerName, playerData);
            }
        } catch (Exception e) {
            System.out.println("Error parsing player data: " + e.getMessage());
        }
    }
    
    // แปลงข้อมูลผู้เล่นจาก JSON object
    private PlayerData parsePlayerDataObject(String dataStr) {
        PlayerData playerData = new PlayerData();
        
        try {
            // แปลง money
            if (dataStr.contains("\"money\":")) {
                int moneyStart = dataStr.indexOf("\"money\":") + 8;
                int moneyEnd = dataStr.indexOf(",", moneyStart);
                if (moneyEnd == -1) moneyEnd = dataStr.indexOf("}", moneyStart);
                String moneyStr = dataStr.substring(moneyStart, moneyEnd).trim();
                playerData.money = Integer.parseInt(moneyStr);
            }
            
            // แปลง level
            if (dataStr.contains("\"level\":")) {
                int levelStart = dataStr.indexOf("\"level\":") + 8;
                int levelEnd = dataStr.indexOf(",", levelStart);
                if (levelEnd == -1) levelEnd = dataStr.indexOf("}", levelStart);
                String levelStr = dataStr.substring(levelStart, levelEnd).trim();
                playerData.level = Integer.parseInt(levelStr);
            }
            
            // แปลง experience
            if (dataStr.contains("\"experience\":")) {
                int expStart = dataStr.indexOf("\"experience\":") + 13;
                int expEnd = dataStr.indexOf(",", expStart);
                if (expEnd == -1) expEnd = dataStr.indexOf("}", expStart);
                String expStr = dataStr.substring(expStart, expEnd).trim();
                playerData.experience = Integer.parseInt(expStr);
            }
            
            // แปลง inventory
            if (dataStr.contains("\"inventory\":")) {
                int invStart = dataStr.indexOf("\"inventory\":") + 12;
                int invEnd = dataStr.indexOf("}", invStart);
                String invStr = dataStr.substring(invStart, invEnd + 1);
                playerData.inventory = parseInventory(invStr);
            }
            
        } catch (Exception e) {
            System.out.println("Error parsing player data object: " + e.getMessage());
        }
        
        return playerData;
    }
    
    // แปลง inventory จาก JSON
    private Map<String, Integer> parseInventory(String invStr) {
        Map<String, Integer> inventory = new HashMap<>();
        
        try {
            if (invStr.startsWith("{") && invStr.endsWith("}")) {
                invStr = invStr.substring(1, invStr.length() - 1);
                String[] items = invStr.split(",");
                for (String item : items) {
                    if (item.contains(":")) {
                        String[] parts = item.split(":");
                        if (parts.length == 2) {
                            String itemName = parts[0].trim().replace("\"", "");
                            int quantity = Integer.parseInt(parts[1].trim());
                            inventory.put(itemName, quantity);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error parsing inventory: " + e.getMessage());
        }
        
        return inventory;
    }
    
    // สร้าง JSON จาก Map
    private String generateJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        
        boolean first = true;
        for (Map.Entry<String, PlayerData> entry : playerDatabase.entrySet()) {
            if (!first) {
                json.append(",\n");
            }
            first = false;
            
            String playerName = entry.getKey();
            PlayerData playerData = entry.getValue();
            
            json.append("  \"").append(playerName).append("\": {\n");
            json.append("    \"money\": ").append(playerData.money).append(",\n");
            json.append("    \"level\": ").append(playerData.level).append(",\n");
            json.append("    \"experience\": ").append(playerData.experience).append(",\n");
            json.append("    \"inventory\": ").append(generateInventoryJSON(playerData.inventory)).append("\n");
            json.append("  }");
        }
        
        json.append("\n}");
        return json.toString();
    }
    
    // สร้าง JSON สำหรับ inventory
    private String generateInventoryJSON(Map<String, Integer> inventory) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        boolean first = true;
        for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
            if (!first) {
                json.append(", ");
            }
            first = false;
            
            json.append("\"").append(entry.getKey()).append("\": ").append(entry.getValue());
        }
        
        json.append("}");
        return json.toString();
    }
    
    // รับข้อมูลผู้เล่น
    public PlayerData getPlayerData(String playerName) {
        return playerDatabase.getOrDefault(playerName, new PlayerData());
    }
    
    // บันทึกข้อมูลผู้เล่น
    public void savePlayerData(String playerName, PlayerData playerData) {
        playerData.playerName = playerName;
        playerDatabase.put(playerName, playerData);
        saveDatabase();
    }
    
    // ตรวจสอบว่าผู้เล่นมีอยู่หรือไม่
    public boolean playerExists(String playerName) {
        return playerDatabase.containsKey(playerName);
    }
    
    // รับรายชื่อผู้เล่นทั้งหมด
    public Set<String> getAllPlayerNames() {
        return playerDatabase.keySet();
    }
    
    // ลบข้อมูลผู้เล่น
    public void deletePlayer(String playerName) {
        playerDatabase.remove(playerName);
        saveDatabase();
    }
}
