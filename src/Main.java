import client.GameClient;
import client.PlayerNameGUI;

public class Main {
    public static void main(String[] args) throws Exception {
        PlayerNameGUI playerNameGUI = new PlayerNameGUI();
        playerNameGUI.setVisible(true);

        
        while (!playerNameGUI.isPlayerSelected()) {
            Thread.sleep(100);
        }

        String selectedPlayer = playerNameGUI.getSelectedPlayerName();
        if (selectedPlayer != null) {
            
            startGameWithPlayer(selectedPlayer, playerNameGUI.getDatabase());
        }
    }
    
    private static void startGameWithPlayer(String playerName, client.JSONDatabase database) {
        try {
            
            GameClient gameClient = new GameClient();
            
            
            if (gameClient.getGamePanel() != null) {
                gameClient.getGamePanel().setCurrentPlayerName(playerName);
                gameClient.getGamePanel().setDatabase(database);
                
                
                gameClient.getGamePanel().loadPlayerDataFromDatabase();
                
                
                gameClient.getGamePanel().repaint();
            }
            
        } catch (Exception e) {
            System.out.println("Error starting game: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
