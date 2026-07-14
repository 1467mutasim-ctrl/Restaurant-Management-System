package restaurant_management_system;import java.sql.*;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/restaurant_db";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "ict-09";
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
    
    public static void initializeDatabase() {
        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();
           
            stmt.execute("CREATE DATABASE IF NOT EXISTS restaurant_db");
            stmt.execute("USE restaurant_db");
            
            
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    reservation_id INT AUTO_INCREMENT PRIMARY KEY,
                    guest_name VARCHAR(100) NOT NULL,
                    table_number INT NOT NULL,
                    contact_no VARCHAR(20) NOT NULL,
                    status VARCHAR(20) DEFAULT 'Active'
                )
            """);
            
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS food_menu (
                    item_id INT AUTO_INCREMENT PRIMARY KEY,
                    item_name VARCHAR(100) NOT NULL,
                    price DECIMAL(10,2) NOT NULL,
                    available BOOLEAN DEFAULT TRUE
                )
            """);
            
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS orders (
                    order_id INT AUTO_INCREMENT PRIMARY KEY,
                    reservation_id INT,
                    item_id INT,
                    quantity INT NOT NULL,
                    FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id),
                    FOREIGN KEY (item_id) REFERENCES food_menu(item_id)
                )
            """);
            
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    user_id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    password VARCHAR(100) NOT NULL,
                    role VARCHAR(20) NOT NULL
                )
            """);
            
            stmt.execute("""
                INSERT IGNORE INTO users (username, password, role) VALUES
                ('manager', 'manager123', 'Manager'),
                ('waiter', 'waiter123', 'Waiter')
            """);
            
            stmt.execute("""
                INSERT IGNORE INTO food_menu (item_name, price) VALUES
                ('Rice', 80.00),
                ('Chicken Curry', 200.00),
                ('Fish Curry', 250.00),
                ('Vegetables', 120.00),
                ('Tea', 25.00),
                ('Coffee', 35.00)
            """);
            
            System.out.println("Database initialized successfully!");
            
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
        }
    }
}