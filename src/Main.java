import client.GameClient;
import client.PlayerNameGUI;

public class Main {
    public static void main(String[] args) throws Exception {
        PlayerNameGUI playerNameGUI = new PlayerNameGUI();
        playerNameGUI.setVisible(true);

        // รอให้ผู้เล่นเลือก
        while (!playerNameGUI.isPlayerSelected()) {
            Thread.sleep(100);
        }

        String selectedPlayer = playerNameGUI.getSelectedPlayerName();
        if (selectedPlayer != null) {
            // เริ่มเกมด้วยชื่อผู้เล่นที่เลือก
            startGameWithPlayer(selectedPlayer, playerNameGUI.getDatabase());
        }
    }
    
    private static void startGameWithPlayer(String playerName, client.JSONDatabase database) {
        try {
            // เริ่มเกม client
            GameClient gameClient = new GameClient();
            
            // ตั้งค่าชื่อผู้เล่นใน GamePanel
            if (gameClient.getGamePanel() != null) {
                gameClient.getGamePanel().setCurrentPlayerName(playerName);
            }
            
        } catch (Exception e) {
            System.out.println("Error starting game: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
