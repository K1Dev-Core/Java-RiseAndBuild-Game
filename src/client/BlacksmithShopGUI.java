package client;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.*;

public class BlacksmithShopGUI extends JDialog implements KeyListener {
    private JPanel shopPanel;
    private JLabel titleLabel;
    private JLabel moneyLabel;
    private JButton closeButton;
    private JPanel itemsPanel;
    private JSONDatabase database;
    private String currentPlayerName;
    private GamePanel gamePanel;
    
    // ไอเทมที่ขายในร้าน
    private String[] shopItems = {
        "Wood", "Stone", "Iron", "Feather", "Coin"
    };
    
    private int[] shopPrices = {
        10, 15, 25, 5, 1
    };
    
    public BlacksmithShopGUI(JFrame parent, GamePanel gamePanel) {
        super(parent, "Blacksmith Shop", true); // modal dialog
        this.gamePanel = gamePanel;
        this.database = gamePanel.getDatabase();
        this.currentPlayerName = gamePanel.getCurrentPlayerName();
        
        setupGUI();
        updateDisplay();
    }
    
    private void setupGUI() {
        setSize(500, 400);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        // สร้าง panel หลัก
        shopPanel = new JPanel();
        shopPanel.setLayout(new BorderLayout());
        shopPanel.setBackground(new Color(40, 35, 30));
        shopPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Title
        titleLabel = new JLabel("BLACKSMITH SHOP", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(255, 215, 0));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        // Money display
        moneyLabel = new JLabel("Money: 0", JLabel.CENTER);
        moneyLabel.setFont(new Font("Arial", Font.BOLD, 16));
        moneyLabel.setForeground(new Color(100, 255, 100));
        moneyLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        // Items panel
        itemsPanel = new JPanel();
        itemsPanel.setLayout(new GridLayout(0, 2, 10, 10));
        itemsPanel.setBackground(new Color(40, 35, 30));
        
        // สร้างปุ่มไอเทม
        for (int i = 0; i < shopItems.length; i++) {
            createItemButton(i);
        }
        
        // Close button
        closeButton = new JButton("Close Shop");
        closeButton.setFont(new Font("Arial", Font.BOLD, 14));
        closeButton.setBackground(new Color(200, 50, 50));
        closeButton.setForeground(Color.WHITE);
        closeButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        closeButton.addActionListener(e -> dispose());
        
        // เพิ่ม components
        shopPanel.add(titleLabel, BorderLayout.NORTH);
        shopPanel.add(moneyLabel, BorderLayout.CENTER);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(40, 35, 30));
        centerPanel.add(new JLabel("Available Items:", JLabel.CENTER), BorderLayout.NORTH);
        centerPanel.add(itemsPanel, BorderLayout.CENTER);
        centerPanel.add(closeButton, BorderLayout.SOUTH);
        
        shopPanel.add(centerPanel, BorderLayout.CENTER);
        
        add(shopPanel);
        addKeyListener(this);
        setFocusable(true);
    }
    
    private void createItemButton(int index) {
        String itemName = shopItems[index];
        int price = shopPrices[index];
        
        JPanel itemPanel = new JPanel();
        itemPanel.setLayout(new BorderLayout());
        itemPanel.setBackground(new Color(60, 55, 50));
        itemPanel.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 2));
        
        // Item name
        JLabel nameLabel = new JLabel(itemName, JLabel.CENTER);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nameLabel.setForeground(Color.WHITE);
        
        // Price
        JLabel priceLabel = new JLabel("Price: " + price + " coins", JLabel.CENTER);
        priceLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        priceLabel.setForeground(new Color(255, 200, 100));
        
        // Buy button
        JButton buyButton = new JButton("BUY");
        buyButton.setFont(new Font("Arial", Font.BOLD, 12));
        buyButton.setBackground(new Color(0, 150, 0));
        buyButton.setForeground(Color.WHITE);
        buyButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        // Action listener
        buyButton.addActionListener(e -> buyItem(itemName, price));
        
        // Layout
        itemPanel.add(nameLabel, BorderLayout.NORTH);
        itemPanel.add(priceLabel, BorderLayout.CENTER);
        itemPanel.add(buyButton, BorderLayout.SOUTH);
        
        itemsPanel.add(itemPanel);
    }
    
    private void buyItem(String itemName, int price) {
        if (database == null || currentPlayerName == null) {
            showMessage("Error: Database not available!");
            return;
        }
        
        // ตรวจสอบเงิน
        JSONDatabase.PlayerData playerData = database.getPlayerData(currentPlayerName);
        if (playerData.money < price) {
            showMessage("Not enough money! You need " + price + " coins.");
            return;
        }
        
        // ซื้อไอเทม
        playerData.money -= price;
        
        // เพิ่มไอเทมในคลัง
        int currentQuantity = playerData.inventory.getOrDefault(itemName, 0);
        playerData.inventory.put(itemName, currentQuantity + 1);
        
        // บันทึกข้อมูล
        database.savePlayerData(currentPlayerName, playerData);
        database.saveDatabase();
        
        // อัปเดตการแสดงผล
        updateDisplay();
        gamePanel.loadPlayerDataFromDatabase();
        
        showMessage("Bought " + itemName + " for " + price + " coins!");
    }
    
    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Shop", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void updateDisplay() {
        if (database == null || currentPlayerName == null) return;
        
        JSONDatabase.PlayerData playerData = database.getPlayerData(currentPlayerName);
        moneyLabel.setText("Money: " + playerData.money + " coins");
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            dispose();
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {}
    
    @Override
    public void keyTyped(KeyEvent e) {}
}
