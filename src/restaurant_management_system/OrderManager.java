package restaurant_management_system;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderManager {
    
    public static class Order {
        private int orderId;
        private int reservationId;
        private int itemId;
        private String itemName;
        private double price;
        private int quantity;
        private double total;
        
        public Order(int orderId, int reservationId, int itemId, String itemName, 
                    double price, int quantity) {
            this.orderId = orderId;
            this.reservationId = reservationId;
            this.itemId = itemId;
            this.itemName = itemName;
            this.price = price;
            this.quantity = quantity;
            this.total = price * quantity;
        }
        
        public int getOrderId() { return orderId; }
        public int getReservationId() { return reservationId; }
        public int getItemId() { return itemId; }
        public String getItemName() { return itemName; }
        public double getPrice() { return price; }
        public int getQuantity() { return quantity; }
        public double getTotal() { return total; }
        
        @Override
        public String toString() {
            return String.format("%s (x%d) - %.2f BDT each = %.2f BDT total",
                itemName, quantity, price, total);
        }
    }
    
    public static boolean addOrder(int reservationId, int itemId, int quantity) {
        String query = "INSERT INTO orders (reservation_id, item_id, quantity) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, reservationId);
            stmt.setInt(2, itemId);
            stmt.setInt(3, quantity);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error adding order: " + e.getMessage());
            return false;
        }
    }
    
    public static List<Order> getOrdersByReservation(int reservationId) {
        List<Order> orders = new ArrayList<>();
        String query = """
            SELECT o.order_id, o.reservation_id, o.item_id, f.item_name, f.price, o.quantity
            FROM orders o
            JOIN food_menu f ON o.item_id = f.item_id
            WHERE o.reservation_id = ?
            ORDER BY o.order_id
        """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, reservationId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                orders.add(new Order(
                    rs.getInt("order_id"),
                    rs.getInt("reservation_id"),
                    rs.getInt("item_id"),
                    rs.getString("item_name"),
                    rs.getDouble("price"),
                    rs.getInt("quantity")
                ));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching orders: " + e.getMessage());
        }
        
        return orders;
    }
    
    public static boolean removeOrder(int orderId) {
        String query = "DELETE FROM orders WHERE order_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, orderId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error removing order: " + e.getMessage());
            return false;
        }
    }
    
    public static boolean updateOrderQuantity(int orderId, int newQuantity) {
        String query = "UPDATE orders SET quantity = ? WHERE order_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, newQuantity);
            stmt.setInt(2, orderId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating order quantity: " + e.getMessage());
            return false;
        }
    }
    
    public static double calculateTotalBill(int reservationId) {
        String query = """
            SELECT SUM(f.price * o.quantity) as total
            FROM orders o
            JOIN food_menu f ON o.item_id = f.item_id
            WHERE o.reservation_id = ?
        """;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, reservationId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("total");
            }
            
        } catch (SQLException e) {
            System.err.println("Error calculating total bill: " + e.getMessage());
        }
        
        return 0.0;
    }
}