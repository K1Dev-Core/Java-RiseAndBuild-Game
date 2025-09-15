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
        setTitle("Rise and Build");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);
        setResizable(false);
        
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(20, 25, 40));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        
        JLabel titleLabel = new JLabel("RISE AND BUILD", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(new Color(255, 215, 0)); 
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
        
        
        JLabel subtitleLabel = new JLabel("Choose Your Character", JLabel.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.ITALIC, 16));
        subtitleLabel.setForeground(new Color(200, 200, 200));
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 40, 0));
        
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setBackground(new Color(30, 35, 50));
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 100), 2),
            BorderFactory.createEmptyBorder(30, 30, 30, 30)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        
        
        JPanel newPlayerPanel = createNewPlayerPanel();
        gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 20, 10);
        contentPanel.add(newPlayerPanel, gbc);
        
        
        JPanel loadPlayerPanel = createLoadPlayerPanel();
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 10, 20, 0);
        contentPanel.add(loadPlayerPanel, gbc);
        
        
        statusLabel = new JLabel("Welcome to Rise and Build!", JLabel.CENTER);
        statusLabel.setForeground(new Color(255, 255, 100));
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        
        
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(subtitleLabel, BorderLayout.CENTER);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);
        
        add(mainPanel);
        addKeyListener(this);
        setFocusable(true);
        
        
        updateExistingPlayers();
        
        
        nameField.requestFocus();
    }
    
    private JPanel createNewPlayerPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(new Color(40, 45, 60));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 150, 0), 2),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        
        JLabel title = new JLabel("CREATE NEW CHARACTER", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(new Color(0, 255, 100));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        inputPanel.setBackground(new Color(40, 45, 60));
        GridBagConstraints gbc = new GridBagConstraints();
        
        
        JLabel nameLabel = new JLabel("Character Name:");
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 10, 0);
        inputPanel.add(nameLabel, gbc);
        
        
        nameField = new JTextField(15);
        nameField.setFont(new Font("Arial", Font.PLAIN, 16));
        nameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        nameField.addKeyListener(this);
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 20, 0);
        inputPanel.add(nameField, gbc);
        
        
        startButton = new JButton("START NEW GAME");
        startButton.setFont(new Font("Arial", Font.BOLD, 14));
        startButton.setBackground(new Color(0, 150, 0));
        startButton.setForeground(Color.WHITE);
        startButton.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        startButton.setFocusPainted(false);
        startButton.addActionListener(this);
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        inputPanel.add(startButton, gbc);
        
        panel.add(title, BorderLayout.NORTH);
        panel.add(inputPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createLoadPlayerPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(new Color(40, 45, 60));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 100, 200), 2),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        
        JLabel title = new JLabel("LOAD CHARACTER", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(new Color(100, 150, 255));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setBackground(new Color(40, 45, 60));
        GridBagConstraints gbc = new GridBagConstraints();
        
        
        JLabel playerLabel = new JLabel("Select Character:");
        playerLabel.setForeground(Color.WHITE);
        playerLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 10, 0);
        contentPanel.add(playerLabel, gbc);
        
        existingPlayersCombo = new JComboBox<>();
        existingPlayersCombo.setFont(new Font("Arial", Font.PLAIN, 16));
        existingPlayersCombo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        existingPlayersCombo.addActionListener(this);
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 20, 0);
        contentPanel.add(existingPlayersCombo, gbc);
        
        
        loadButton = new JButton("LOAD GAME");
        loadButton.setFont(new Font("Arial", Font.BOLD, 14));
        loadButton.setBackground(new Color(0, 100, 200));
        loadButton.setForeground(Color.WHITE);
        loadButton.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        loadButton.setFocusPainted(false);
        loadButton.addActionListener(this);
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        contentPanel.add(loadButton, gbc);
        
        panel.add(title, BorderLayout.NORTH);
        panel.add(contentPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void updateExistingPlayers() {
        existingPlayersCombo.removeAllItems();
        existingPlayersCombo.addItem("Choose a character...");
        
        Set<String> playerNames = database.getAllPlayerNames();
        if (playerNames.isEmpty()) {
            existingPlayersCombo.addItem("No saved characters");
            loadButton.setEnabled(false);
        } else {
            for (String playerName : playerNames) {
                existingPlayersCombo.addItem(playerName);
            }
            loadButton.setEnabled(true);
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
            if (selected != null && !selected.equals("Choose a character...") && !selected.equals("No saved characters")) {
                statusLabel.setText("Selected: " + selected);
                statusLabel.setForeground(new Color(100, 255, 100));
                loadButton.setEnabled(true);
            } else {
                statusLabel.setText("Please select a character to load");
                statusLabel.setForeground(new Color(255, 200, 100));
                loadButton.setEnabled(!selected.equals("No saved characters"));
            }
        }
    }
    
    private void startNewGame() {
        String playerName = nameField.getText().trim();
        
        if (playerName.isEmpty()) {
            statusLabel.setText("Please enter a character name!");
            statusLabel.setForeground(new Color(255, 100, 100));
            nameField.requestFocus();
            return;
        }
        
        if (playerName.length() < 3) {
            statusLabel.setText("Character name must be at least 3 characters!");
            statusLabel.setForeground(new Color(255, 100, 100));
            nameField.requestFocus();
            return;
        }
        
        if (database.playerExists(playerName)) {
            statusLabel.setText("Character name already exists! Choose a different name.");
            statusLabel.setForeground(new Color(255, 100, 100));
            nameField.selectAll();
            nameField.requestFocus();
            return;
        }
        
        
        JSONDatabase.PlayerData newPlayer = new JSONDatabase.PlayerData();
        newPlayer.playerName = playerName;
        newPlayer.money = 100; 
        newPlayer.level = 1;
        newPlayer.experience = 0;
        
        database.savePlayerData(playerName, newPlayer);
        selectedPlayerName = playerName;
        isPlayerSelected = true;
        
        statusLabel.setText("Character created: " + playerName + " - Starting game...");
        statusLabel.setForeground(new Color(100, 255, 100));
        
        
        Timer timer = new Timer(1500, e -> {
            setVisible(false);
            dispose();
        });
        timer.setRepeats(false);
        timer.start();
    }
    
    private void loadExistingGame() {
        String selectedPlayer = (String) existingPlayersCombo.getSelectedItem();
        
        if (selectedPlayer == null || selectedPlayer.equals("Choose a character...") || selectedPlayer.equals("No saved characters")) {
            statusLabel.setText("Please select a character to load!");
            statusLabel.setForeground(new Color(255, 100, 100));
            return;
        }
        
        selectedPlayerName = selectedPlayer;
        isPlayerSelected = true;
        
        statusLabel.setText("Loading character: " + selectedPlayer + " - Starting game...");
        statusLabel.setForeground(new Color(100, 255, 100));
        
        
        Timer timer = new Timer(1500, e -> {
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
        
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        
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
