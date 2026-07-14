package restaurant_management_system;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FoodMenuManager {
    
    public static class FoodItem {
        private int itemId;
        private String itemName;
        private double price;
        private boolean available;
        
        public FoodItem(int itemId, String itemName, double price, boolean available) {
            this.itemId = itemId;
            this.itemName = itemName;
            this.price = price;
            this.available = available;
        }
        
        // Getters
        public int getItemId() { return itemId; }
        public String getItemName() { return itemName; }
        public double getPrice() { return price; }
        public boolean isAvailable() { return available; }
        
        @Override
        public String toString() {
            return String.format("ID: %d | %s - %.2f BDT %s", 
                itemId, itemName, price, available ? "(Available)" : "(Unavailable)");
        }
    }
    
    public static boolean addFoodItem(String itemName, double price) {
        String query = "INSERT INTO food_menu (item_name, price) VALUES (?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, itemName);
            stmt.setDouble(2, price);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error adding food item: " + e.getMessage());
            return false;
        }
    }
    
    public static boolean removeFoodItem(int itemId) {
        String query = "DELETE FROM food_menu WHERE item_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, itemId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error removing food item: " + e.getMessage());
            return false;
        }
    }
    
    public static boolean updateFoodPrice(int itemId, double newPrice) {
        String query = "UPDATE food_menu SET price = ? WHERE item_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setDouble(1, newPrice);
            stmt.setInt(2, itemId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating food price: " + e.getMessage());
            return false;
        }
    }
    
    public static List<FoodItem> getAllFoodItems() {
        List<FoodItem> items = new ArrayList<>();
        String query = "SELECT * FROM food_menu ORDER BY item_id";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                items.add(new FoodItem(
                    rs.getInt("item_id"),
                    rs.getString("item_name"),
                    rs.getDouble("price"),
                    rs.getBoolean("available")
                ));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching food items: " + e.getMessage());
        }
        
        return items;
    }
    
    public static FoodItem getFoodItem(int itemId) {
        String query = "SELECT * FROM food_menu WHERE item_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, itemId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new FoodItem(
                    rs.getInt("item_id"),
                    rs.getString("item_name"),
                    rs.getDouble("price"),
                    rs.getBoolean("available")
                );
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching food item: " + e.getMessage());
        }
        
        return null;
    }
}
