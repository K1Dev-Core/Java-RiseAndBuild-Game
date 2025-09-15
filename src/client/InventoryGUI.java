package client;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

public class InventoryGUI extends JDialog implements KeyListener {
    private boolean isVisible = false;
    private JPanel inventoryPanel;
    private List<InventoryItem> items;
    private JLabel[] itemLabels;
    private JLabel titleLabel; 
    private static final int INVENTORY_SIZE = 8; 
    private static final int ITEMS_PER_ROW = 3; 
    
    public InventoryGUI(JFrame parent) {
        super(parent, "Inventory", false); 
        initializeInventory();
        setupGUI();
    }
    
    private void initializeInventory() {
        items = new ArrayList<>();
        
    }
    
    private void setupGUI() {
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        setSize(400, 300);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width - getWidth() - 100; 
        int y = (screenSize.height - getHeight()) / 2; 
        setLocation(x, y);
        
        setResizable(false);
        setUndecorated(true);
        
        
        inventoryPanel = new JPanel();
        inventoryPanel.setLayout(new BorderLayout());
        inventoryPanel.setBackground(new Color(50, 50, 50));
        
        
        titleLabel = new JLabel("INVENTORY", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
      
        
        
        JPanel itemsGrid = new JPanel();
        itemsGrid.setLayout(new GridLayout(4, ITEMS_PER_ROW, 5, 5));
        itemsGrid.setBackground(new Color(50, 50, 50));
        itemsGrid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        
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
            
            
            final int index = i;
            itemLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    System.out.println("Mouse entered item slot " + index);
                    if (index < items.size() && items.get(index) != null) {
                        itemLabel.setBackground(new Color(100, 150, 100)); 
                        itemLabel.setBorder(BorderFactory.createLineBorder(Color.YELLOW, 2));
                        
                        
                        InventoryItem item = items.get(index);
                        String itemText = item.getName() + " x" + item.getQuantity();
                        System.out.println("Setting itemInfoLabel to: " + itemText);
                        titleLabel.setText(itemText);
                        titleLabel.repaint(); 
                    } else {
                        System.out.println("No item at index " + index + " or item is null");
                    }
                }
                
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    System.out.println("Mouse exited item slot " + index);
                    if (index < items.size() && items.get(index) != null) {
                        itemLabel.setBackground(new Color(80, 80, 80)); 
                        itemLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
                        
                        
                        System.out.println("Clearing itemInfoLabel");
                        titleLabel.setText("INVENTORY ");
                        titleLabel.repaint(); 
                    }
                }
                
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (index < items.size() && items.get(index) != null) {
                        useItem(index);
                    }
                }
            });
            
            itemLabels[i] = itemLabel;
            itemsGrid.add(itemLabel);
        }
        
        
        updateItemDisplay();
        
        
        inventoryPanel.add(titleLabel, BorderLayout.NORTH);
    
        inventoryPanel.add(itemsGrid, BorderLayout.SOUTH);
        
        add(inventoryPanel);
        addKeyListener(this);
        setFocusable(true);
        
        
        setVisible(false);
    }
    
    private void updateItemDisplay() {
        System.out.println("Updating item display. Items count: " + items.size());
        
        
        for (JLabel label : itemLabels) {
            label.setText("");
            label.setIcon(null);
         
        }
        
        
        for (int i = 0; i < Math.min(items.size(), INVENTORY_SIZE); i++) {
            InventoryItem item = items.get(i);
            JLabel label = itemLabels[i];
            
            if (item != null && item.getQuantity() > 0) {
                System.out.println("Displaying item " + i + ": " + item.getName() + " x" + item.getQuantity());
                
                
                
                
                boolean hasImage = false;
                try {
                    String imagePath = item.getImagePath();
                    if (imagePath != null && !imagePath.isEmpty()) {
                        ImageIcon icon = new ImageIcon(imagePath);
                        if (icon.getIconWidth() > 0) {
                            
                            Image img = icon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                            label.setIcon(new ImageIcon(img));
                            hasImage = true;
                            System.out.println("Loaded image for " + item.getName());
                        } else {
                            label.setIcon(null);
                        }
                    } else {
                        label.setIcon(null);
                    }
                } catch (Exception e) {
                    System.out.println("Error loading image for " + item.getName() + ": " + e.getMessage());
                    label.setIcon(null);
                }
                
                
                if (!hasImage) {
                    label.setText(item.getName() + " x" + item.getQuantity());
                } else {
                    label.setText(""); 
                }
                
                
                label.setBackground(new Color(80, 80, 80)); 
            }
        }
    }
    
    public void toggleVisibility() {
        System.out.println("Inventory toggleVisibility called. Current visible: " + isVisible);
        isVisible = !isVisible;
        setVisible(isVisible);
        
        if (isVisible) {
            System.out.println("Opening inventory...");
            
            updateItemDisplay();
            requestFocus();
            toFront();
        } else {
            System.out.println("Closing inventory...");
      
            
            if (getParent() != null) {
                getParent().requestFocus();
            }
        }
    }
    
    public boolean isInventoryVisible() {
        return isVisible;
    }
    
    public void addItem(String name, int quantity) {
        
        for (InventoryItem item : items) {
            if (item.getName().equals(name)) {
                item.addQuantity(quantity);
                updateItemDisplay();
                return;
            }
        }
        
        
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
        System.out.println("Loading items from map: " + itemsMap);
        items.clear();
        for (Map.Entry<String, Integer> entry : itemsMap.entrySet()) {
            String imagePath = "assets/sprites/items/" + entry.getKey().toLowerCase().replace(" ", "_") + ".png";
            System.out.println("Adding item: " + entry.getKey() + " x" + entry.getValue() + " with image: " + imagePath);
            items.add(new InventoryItem(entry.getKey(), entry.getValue(), imagePath));
        }
        System.out.println("Total items loaded: " + items.size());
        updateItemDisplay();
    }
    
    private void useItem(int index) {
        if (index >= 0 && index < items.size()) {
            InventoryItem item = items.get(index);
            if (item != null && item.getQuantity() > 0) {
                
                
                if (getParent() instanceof GamePanel) {
                    ((GamePanel) getParent()).showNotification("Used: " + item.getName());
                }
            }
        }
    }
    
    
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_I) {
            toggleVisibility();
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        
    }
    
    
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
        
        public void setQuantity(int quantity) {
            this.quantity = Math.max(0, quantity);
        }
        
        public String getImagePath() {
            return imagePath;
        }
    }
}
