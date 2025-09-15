package client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Set;
import javax.swing.*;

public class PlayerNameGUI extends JFrame implements ActionListener, KeyListener {
    private JTextField nameField;
    private JButton startButton;
    private JButton loadButton;
    private JComboBox<String> existingPlayersCombo;
    private JLabel statusLabel;
    private JSONDatabase database;
    private String selectedPlayerName;
    private boolean isPlayerSelected = false;
    
    public PlayerNameGUI() {
        database = new JSONDatabase();
        setupGUI();
    }
    
    private void setupGUI() {
        setTitle("Rise and Build - Player Selection");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setResizable(false);
        
        // สร้าง panel หลัก
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(50, 50, 50));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // สร้าง title
        JLabel titleLabel = new JLabel("RISE AND BUILD", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        // สร้าง panel สำหรับ input
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        inputPanel.setBackground(new Color(50, 50, 50));
        GridBagConstraints gbc = new GridBagConstraints();
        
        // ชื่อผู้เล่นใหม่
        JLabel newPlayerLabel = new JLabel("New Player Name:");
        newPlayerLabel.setForeground(Color.WHITE);
        newPlayerLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 0, 5, 10);
        inputPanel.add(newPlayerLabel, gbc);
        
        nameField = new JTextField(20);
        nameField.setFont(new Font("Arial", Font.PLAIN, 14));
        nameField.addKeyListener(this);
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        inputPanel.add(nameField, gbc);
        
        startButton = new JButton("Start New Game");
        startButton.setFont(new Font("Arial", Font.BOLD, 12));
        startButton.setBackground(new Color(0, 150, 0));
        startButton.setForeground(Color.WHITE);
        startButton.addActionListener(this);
        gbc.gridx = 2; gbc.gridy = 0; gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 10, 5, 0);
        inputPanel.add(startButton, gbc);
        
        // เส้นแบ่ง
        JSeparator separator = new JSeparator();
        separator.setForeground(Color.GRAY);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(15, 0, 15, 0);
        inputPanel.add(separator, gbc);
        
        // ผู้เล่นที่มีอยู่
        JLabel existingPlayerLabel = new JLabel("Load Existing Player:");
        existingPlayerLabel.setForeground(Color.WHITE);
        existingPlayerLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 0, 5, 10);
        inputPanel.add(existingPlayerLabel, gbc);
        
        existingPlayersCombo = new JComboBox<>();
        existingPlayersCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        existingPlayersCombo.addActionListener(this);
        updateExistingPlayers();
        gbc.gridx = 1; gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        inputPanel.add(existingPlayersCombo, gbc);
        
        loadButton = new JButton("Load Game");
        loadButton.setFont(new Font("Arial", Font.BOLD, 12));
        loadButton.setBackground(new Color(0, 100, 200));
        loadButton.setForeground(Color.WHITE);
        loadButton.addActionListener(this);
        gbc.gridx = 2; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 10, 5, 0);
        inputPanel.add(loadButton, gbc);
        
        // Status label
        statusLabel = new JLabel("Enter your player name to start", JLabel.CENTER);
        statusLabel.setForeground(Color.YELLOW);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(15, 0, 0, 0);
        inputPanel.add(statusLabel, gbc);
        
        // เพิ่ม components
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(inputPanel, BorderLayout.CENTER);
        
        add(mainPanel);
        addKeyListener(this);
        setFocusable(true);
        
        // Focus ที่ name field
        nameField.requestFocus();
    }
    
    private void updateExistingPlayers() {
        existingPlayersCombo.removeAllItems();
        existingPlayersCombo.addItem("Select a player...");
        
        Set<String> playerNames = database.getAllPlayerNames();
        for (String playerName : playerNames) {
            existingPlayersCombo.addItem(playerName);
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startButton) {
            startNewGame();
        } else if (e.getSource() == loadButton) {
            loadExistingGame();
        } else if (e.getSource() == existingPlayersCombo) {
            String selected = (String) existingPlayersCombo.getSelectedItem();
            if (selected != null && !selected.equals("Select a player...")) {
                statusLabel.setText("Selected: " + selected);
                statusLabel.setForeground(Color.GREEN);
            }
        }
    }
    
    private void startNewGame() {
        String playerName = nameField.getText().trim();
        
        if (playerName.isEmpty()) {
            statusLabel.setText("Please enter a player name!");
            statusLabel.setForeground(Color.RED);
            return;
        }
        
        if (database.playerExists(playerName)) {
            statusLabel.setText("Player name already exists! Choose a different name.");
            statusLabel.setForeground(Color.RED);
            return;
        }
        
        // สร้างผู้เล่นใหม่
        JSONDatabase.PlayerData newPlayer = new JSONDatabase.PlayerData();
        newPlayer.playerName = playerName;
        newPlayer.money = 100; // เงินเริ่มต้น
        newPlayer.level = 1;
        newPlayer.experience = 0;
        newPlayer.inventory.put("Wood", 10);
        newPlayer.inventory.put("Stone", 5);
        newPlayer.inventory.put("Iron", 2);
        
        database.savePlayerData(playerName, newPlayer);
        selectedPlayerName = playerName;
        isPlayerSelected = true;
        
        statusLabel.setText("New player created: " + playerName);
        statusLabel.setForeground(Color.GREEN);
        
        // ปิด GUI หลังจาก 1 วินาที
        Timer timer = new Timer(1000, e -> {
            setVisible(false);
            dispose();
        });
        timer.setRepeats(false);
        timer.start();
    }
    
    private void loadExistingGame() {
        String selectedPlayer = (String) existingPlayersCombo.getSelectedItem();
        
        if (selectedPlayer == null || selectedPlayer.equals("Select a player...")) {
            statusLabel.setText("Please select a player to load!");
            statusLabel.setForeground(Color.RED);
            return;
        }
        
        selectedPlayerName = selectedPlayer;
        isPlayerSelected = true;
        
        statusLabel.setText("Loading player: " + selectedPlayer);
        statusLabel.setForeground(Color.GREEN);
        
        // ปิด GUI หลังจาก 1 วินาที
        Timer timer = new Timer(1000, e -> {
            setVisible(false);
            dispose();
        });
        timer.setRepeats(false);
        timer.start();
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (nameField.hasFocus()) {
                startNewGame();
            }
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        // ไม่ต้องทำอะไร
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // ไม่ต้องทำอะไร
    }
    
    public String getSelectedPlayerName() {
        return selectedPlayerName;
    }
    
    public boolean isPlayerSelected() {
        return isPlayerSelected;
    }
    
    public JSONDatabase getDatabase() {
        return database;
    }
}
