package client;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

public class InventoryGUI extends JFrame implements KeyListener {
    private boolean isVisible = false;
    private JPanel inventoryPanel;
    private List<InventoryItem> items;
    private JLabel[] itemLabels;
    private static final int INVENTORY_SIZE = 20; // จำนวนช่องเก็บของ
    private static final int ITEMS_PER_ROW = 5; // จำนวนไอเท็มต่อแถว
    
    public InventoryGUI() {
        initializeInventory();
        setupGUI();
    }
    
    private void initializeInventory() {
        items = new ArrayList<>();
        // เพิ่มไอเท็มเริ่มต้น
        items.add(new InventoryItem("Wood", 10, "assets/sprites/items/wood.png"));
        items.add(new InventoryItem("Stone", 5, "assets/sprites/items/stone.png"));
        items.add(new InventoryItem("Iron", 2, "assets/sprites/items/iron.png"));
    }
    
    private void setupGUI() {
        setTitle("Inventory");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setResizable(false);
        
        // สร้าง panel หลัก
        inventoryPanel = new JPanel();
        inventoryPanel.setLayout(new BorderLayout());
        inventoryPanel.setBackground(new Color(50, 50, 50));
        
        // สร้าง title
        JLabel titleLabel = new JLabel("INVENTORY", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        // สร้าง grid สำหรับไอเท็ม
        JPanel itemsGrid = new JPanel();
        itemsGrid.setLayout(new GridLayout(4, ITEMS_PER_ROW, 5, 5));
        itemsGrid.setBackground(new Color(50, 50, 50));
        itemsGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // สร้างช่องเก็บของ
        itemLabels = new JLabel[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            JLabel itemLabel = new JLabel();
            itemLabel.setPreferredSize(new Dimension(60, 60));
            itemLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
            itemLabel.setBackground(new Color(80, 80, 80));
            itemLabel.setOpaque(true);
            itemLabel.setHorizontalAlignment(JLabel.CENTER);
            itemLabel.setVerticalAlignment(JLabel.CENTER);
            itemLabel.setFont(new Font("Arial", Font.BOLD, 10));
            itemLabel.setForeground(Color.WHITE);
            
            itemLabels[i] = itemLabel;
            itemsGrid.add(itemLabel);
        }
        
        // อัปเดตการแสดงผลไอเท็ม
        updateItemDisplay();
        
        // เพิ่ม components
        inventoryPanel.add(titleLabel, BorderLayout.NORTH);
        inventoryPanel.add(itemsGrid, BorderLayout.CENTER);
        
        add(inventoryPanel);
        addKeyListener(this);
        setFocusable(true);
        
        // ซ่อน GUI เริ่มต้น
        setVisible(false);
    }
    
    private void updateItemDisplay() {
        // ล้างการแสดงผลทั้งหมด
        for (JLabel label : itemLabels) {
            label.setText("");
            label.setIcon(null);
            label.setToolTipText("");
        }
        
        // แสดงไอเท็มที่มี
        for (int i = 0; i < Math.min(items.size(), INVENTORY_SIZE); i++) {
            InventoryItem item = items.get(i);
            JLabel label = itemLabels[i];
            
            if (item != null && item.getQuantity() > 0) {
                label.setText(item.getName() + " x" + item.getQuantity());
                label.setToolTipText(item.getName() + " - " + item.getQuantity() + " pieces");
                
                // ตั้งค่าสีตามประเภทไอเท็ม
                if (item.getName().equals("Wood")) {
                    label.setBackground(new Color(139, 69, 19)); // สีน้ำตาล
                } else if (item.getName().equals("Stone")) {
                    label.setBackground(new Color(128, 128, 128)); // สีเทา
                } else if (item.getName().equals("Iron")) {
                    label.setBackground(new Color(192, 192, 192)); // สีเงิน
                } else {
                    label.setBackground(new Color(100, 100, 100)); // สีเทาเข้ม
                }
            }
        }
    }
    
    public void toggleVisibility() {
        isVisible = !isVisible;
        setVisible(isVisible);
        
        if (isVisible) {
            updateItemDisplay();
            requestFocus();
        }
    }
    
    public boolean isInventoryVisible() {
        return isVisible;
    }
    
    public void addItem(String name, int quantity) {
        // หาไอเท็มที่มีอยู่แล้ว
        for (InventoryItem item : items) {
            if (item.getName().equals(name)) {
                item.addQuantity(quantity);
                updateItemDisplay();
                return;
            }
        }
        
        // ถ้าไม่มีไอเท็มนี้ ให้เพิ่มใหม่
        if (items.size() < INVENTORY_SIZE) {
            items.add(new InventoryItem(name, quantity, "assets/sprites/items/" + name.toLowerCase() + ".png"));
            updateItemDisplay();
        }
    }
    
    public void removeItem(String name, int quantity) {
        for (InventoryItem item : items) {
            if (item.getName().equals(name)) {
                item.removeQuantity(quantity);
                if (item.getQuantity() <= 0) {
                    items.remove(item);
                }
                updateItemDisplay();
                break;
            }
        }
    }
    
    public int getItemQuantity(String name) {
        for (InventoryItem item : items) {
            if (item.getName().equals(name)) {
                return item.getQuantity();
            }
        }
        return 0;
    }
    
    public Map<String, Integer> getAllItems() {
        Map<String, Integer> allItems = new HashMap<>();
        for (InventoryItem item : items) {
            if (item.getQuantity() > 0) {
                allItems.put(item.getName(), item.getQuantity());
            }
        }
        return allItems;
    }
    
    public void loadItems(Map<String, Integer> itemsMap) {
        items.clear();
        for (Map.Entry<String, Integer> entry : itemsMap.entrySet()) {
            items.add(new InventoryItem(entry.getKey(), entry.getValue(), 
                "assets/sprites/items/" + entry.getKey().toLowerCase().replace(" ", "_") + ".png"));
        }
        updateItemDisplay();
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_TAB) {
            toggleVisibility();
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
    
    // Inner class สำหรับไอเท็มในคลังเก็บของ
    public static class InventoryItem {
        private String name;
        private int quantity;
        private String imagePath;
        
        public InventoryItem(String name, int quantity, String imagePath) {
            this.name = name;
            this.quantity = quantity;
            this.imagePath = imagePath;
        }
        
        public String getName() {
            return name;
        }
        
        public int getQuantity() {
            return quantity;
        }
        
        public void addQuantity(int amount) {
            this.quantity += amount;
        }
        
        public void removeQuantity(int amount) {
            this.quantity = Math.max(0, this.quantity - amount);
        }
        
        public String getImagePath() {
            return imagePath;
        }
    }
}
