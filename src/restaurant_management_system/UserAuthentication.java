
package restaurant_management_system;

import java.sql.*;

public class UserAuthentication {
    
    public static class User {
        private String username;
        private String role;
        
        public User(String username, String role) {
            this.username = username;
            this.role = role;
        }
        
        public String getUsername() { return username; }
        public String getRole() { return role; }
    }
    
    public static User authenticateUser(String username, String password) {
        String query = "SELECT username, role FROM users WHERE username = ? AND password = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, username);
            stmt.setString(2, password);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new User(rs.getString("username"), rs.getString("role"));
            }
            
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
        }
        
        return null;
    }
    
    public static boolean isManager(User user) {
        return user != null && "Manager".equals(user.getRole());
    }
    
    public static boolean isWaiter(User user) {
        return user != null && "Waiter".equals(user.getRole());
    }
}