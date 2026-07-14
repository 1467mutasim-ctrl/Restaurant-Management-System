package restaurant_management_system;

import java.util.List;

public class BillingSystem {
    
    public static class Bill {
        private ReservationManager.Reservation reservation;
        private List<OrderManager.Order> orders;
        private double subtotal;
        private double tax;
        private double total;
        
        public Bill(ReservationManager.Reservation reservation, List<OrderManager.Order> orders) {
            this.reservation = reservation;
            this.orders = orders;
            this.subtotal = calculateSubtotal();
            this.tax = subtotal * 0.05;
            this.total = subtotal + tax;
        }
        
        private double calculateSubtotal() {
            return orders.stream()
                        .mapToDouble(OrderManager.Order::getTotal)
                        .sum();
        }
        
        public ReservationManager.Reservation getReservation() { return reservation; }
        public List<OrderManager.Order> getOrders() { return orders; }
        public double getSubtotal() { return subtotal; }
        public double getTax() { return tax; }
        public double getTotal() { return total; }
        
        public String generateBillString() {
            StringBuilder bill = new StringBuilder();
            bill.append("==========================================\n");
            bill.append("           RESTAURANT BILL\n");
            bill.append("==========================================\n\n");
            
            bill.append("Reservation Details:\n");
            bill.append("Reservation ID: ").append(reservation.getReservationId()).append("\n");
            bill.append("Guest Name: ").append(reservation.getGuestName()).append("\n");
            bill.append("Table Number: ").append(reservation.getTableNumber()).append("\n");
            bill.append("Contact: ").append(reservation.getContactNo()).append("\n\n");
            
            bill.append("Order Details:\n");
            bill.append("------------------------------------------\n");
            
            for (OrderManager.Order order : orders) {
                bill.append(String.format("%-20s x%d  %8.2f BDT\n", 
                    order.getItemName(), order.getQuantity(), order.getTotal()));
            }
            
            bill.append("------------------------------------------\n");
            bill.append(String.format("Subtotal:           %12.2f BDT\n", subtotal));
            bill.append(String.format("Tax (5%%):           %12.2f BDT\n", tax));
            bill.append("==========================================\n");
            bill.append(String.format("TOTAL:              %12.2f BDT\n", total));
            bill.append("==========================================\n\n");
            bill.append("Thank you for dining with us!\n");
            
            return bill.toString();
        }
    }
    
    public static Bill generateBill(int reservationId) {
        ReservationManager.Reservation reservation = ReservationManager.getReservationById(reservationId);
        
        if (reservation == null) {
            System.err.println("Reservation not found!");
            return null;
        }
        
        List<OrderManager.Order> orders = OrderManager.getOrdersByReservation(reservationId);
        
        if (orders.isEmpty()) {
            System.err.println("No orders found for this reservation!");
            return null;
        }
        
        return new Bill(reservation, orders);
    }
    
    public static void printBill(int reservationId) {
        Bill bill = generateBill(reservationId);
        if (bill != null) {
            System.out.println(bill.generateBillString());
        }
    }
    
    public static boolean completeBilling(int reservationId) {
        Bill bill = generateBill(reservationId);
        if (bill != null) {
            boolean updated = ReservationManager.updateReservationStatus(reservationId, "Completed");
            if (updated) {
                System.out.println("Billing completed successfully!");
                System.out.println(bill.generateBillString());
                return true;
            }
        }
        return false;
    }
}