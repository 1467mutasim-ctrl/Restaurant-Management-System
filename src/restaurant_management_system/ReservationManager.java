package restaurant_management_system;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationManager {
    
    public static class Reservation {
        private int reservationId;
        private String guestName;
        private int tableNumber;
        private String contactNo;
        private String status;
        
        public Reservation(int reservationId, String guestName, int tableNumber, 
                          String contactNo, String status) {
            this.reservationId = reservationId;
            this.guestName = guestName;
            this.tableNumber = tableNumber;
            this.contactNo = contactNo;
            this.status = status;
        }
        
        public int getReservationId() { return reservationId; }
        public String getGuestName() { return guestName; }
        public int getTableNumber() { return tableNumber; }
        public String getContactNo() { return contactNo; }
        public String getStatus() { return status; }
        
        @Override
        public String toString() {
            return String.format("ID: %d | Guest: %s | Table: %d | Contact: %s | Status: %s",
                reservationId, guestName, tableNumber, contactNo, status);
        }
    }
    
    public static int createReservation(String guestName, int tableNumber, String contactNo) {
        String query = "INSERT INTO reservations (guest_name, table_number, contact_no) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, guestName);
            stmt.setInt(2, tableNumber);
            stmt.setString(3, contactNo);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error creating reservation: " + e.getMessage());
        }
        
        return -1;
    }
    
    public static List<Reservation> getAllReservations() {
        List<Reservation> reservations = new ArrayList<>();
        String query = "SELECT * FROM reservations ORDER BY reservation_id";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                reservations.add(new Reservation(
                    rs.getInt("reservation_id"),
                    rs.getString("guest_name"),
                    rs.getInt("table_number"),
                    rs.getString("contact_no"),
                    rs.getString("status")
                ));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching reservations: " + e.getMessage());
        }
        
        return reservations;
    }
    
    public static List<Reservation> getActiveReservations() {
        List<Reservation> reservations = new ArrayList<>();
        String query = "SELECT * FROM reservations WHERE status = 'Active' ORDER BY reservation_id";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                reservations.add(new Reservation(
                    rs.getInt("reservation_id"),
                    rs.getString("guest_name"),
                    rs.getInt("table_number"),
                    rs.getString("contact_no"),
                    rs.getString("status")
                ));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching active reservations: " + e.getMessage());
        }
        
        return reservations;
    }
    
    public static Reservation getReservationById(int reservationId) {
        String query = "SELECT * FROM reservations WHERE reservation_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, reservationId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new Reservation(
                    rs.getInt("reservation_id"),
                    rs.getString("guest_name"),
                    rs.getInt("table_number"),
                    rs.getString("contact_no"),
                    rs.getString("status")
                );
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching reservation: " + e.getMessage());
        }
        
        return null;
    }
    
    public static boolean updateReservationStatus(int reservationId, String status) {
        String query = "UPDATE reservations SET status = ? WHERE reservation_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, status);
            stmt.setInt(2, reservationId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating reservation status: " + e.getMessage());
            return false;
        }
    }

    public static boolean deleteReservation(int reservationId) {
        String query = "DELETE FROM reservations WHERE reservation_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, reservationId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting reservation: " + e.getMessage());
            return false;
        }
    }
}
