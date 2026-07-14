package restaurant_management_system;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** A small dependency-free HTTP server with in-memory data. */
public final class RestaurantWebServer {
    private static final int PORT = 8080;
    private static final Path WEB_ROOT = Paths.get("web").toAbsolutePath().normalize();
    private static final AtomicInteger RESERVATION_IDS = new AtomicInteger(2);
    private static final AtomicInteger ITEM_IDS = new AtomicInteger(7);
    private static final AtomicInteger ORDER_IDS = new AtomicInteger(1);
    private static final List<Reservation> reservations = new ArrayList<>();
    private static final List<MenuItem> menu = new ArrayList<>();
    private static final List<Order> orders = new ArrayList<>();

    static {
        menu.add(new MenuItem(1, "Rice", 80)); menu.add(new MenuItem(2, "Chicken Curry", 200));
        menu.add(new MenuItem(3, "Fish Curry", 250)); menu.add(new MenuItem(4, "Vegetables", 120));
        menu.add(new MenuItem(5, "Tea", 25)); menu.add(new MenuItem(6, "Coffee", 35));
        reservations.add(new Reservation(1, "Ayesha Rahman", 4, "01700-123456", "Active"));
        orders.add(new Order(ORDER_IDS.getAndIncrement(), 1, 2, 1));
        orders.add(new Order(ORDER_IDS.getAndIncrement(), 1, 5, 2));
    }

    private RestaurantWebServer() { }

    public static void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);
            server.createContext("/api/login", RestaurantWebServer::login);
            server.createContext("/api/menu", RestaurantWebServer::menu);
            server.createContext("/api/reservations", RestaurantWebServer::reservations);
            server.createContext("/api/orders", RestaurantWebServer::orders);
            server.createContext("/api/bill", RestaurantWebServer::bill);
            server.createContext("/", RestaurantWebServer::staticFile);
            server.setExecutor(Executors.newCachedThreadPool()); server.start();
            URI address = URI.create("http://localhost:" + PORT);
            System.out.println("Restaurant Management System is running at " + address);
            System.out.println("Close this window or press Ctrl+C to stop it.");
            if (Desktop.isDesktopSupported()) {
                try { Desktop.getDesktop().browse(address); }
                catch (IOException ex) { System.out.println("Open " + address + " in your browser."); }
            }
        } catch (IOException ex) { System.err.println("Could not start the browser interface: " + ex.getMessage()); }
    }

    private static void login(HttpExchange x) throws IOException {
        if (!method(x, "POST")) return; Map<String, String> p = form(x);
        String u = p.getOrDefault("username", ""), pass = p.getOrDefault("password", "");
        String role = "manager".equals(u) && "manager123".equals(pass) ? "Manager"
                : "waiter".equals(u) && "waiter123".equals(pass) ? "Waiter" : null;
        if (role == null) json(x, 401, error("Invalid username or password"));
        else json(x, 200, "{\"username\":\"" + escape(u) + "\",\"role\":\"" + role + "\"}");
    }

    private static synchronized void menu(HttpExchange x) throws IOException {
        try {
            if ("GET".equals(x.getRequestMethod())) {
                StringBuilder out = new StringBuilder("[");
                for (MenuItem i : menu) { if (out.length() > 1) out.append(','); out.append(itemJson(i)); }
                json(x, 200, out.append(']').toString());
            } else if ("POST".equals(x.getRequestMethod())) {
                Map<String, String> p = form(x); MenuItem i = new MenuItem(ITEM_IDS.getAndIncrement(), required(p, "name"), positiveDouble(p, "price"));
                menu.add(i); json(x, 201, itemJson(i));
            } else if ("PUT".equals(x.getRequestMethod())) {
                Map<String, String> p = form(x); MenuItem i = findItem(integer(p, "id"));
                if (i == null) json(x, 404, error("Menu item not found")); else { i.price = positiveDouble(p, "price"); json(x, 200, itemJson(i)); }
            } else if ("DELETE".equals(x.getRequestMethod())) {
                int id = integer(query(x), "id");
                if (orders.stream().anyMatch(o -> o.itemId == id)) { json(x, 409, error("This item is already used in an order")); return; }
                boolean ok = menu.removeIf(i -> i.id == id); json(x, ok ? 200 : 404, ok ? "{\"ok\":true}" : error("Menu item not found"));
            } else method(x, "GET");
        } catch (IllegalArgumentException e) { json(x, 400, error(e.getMessage())); }
    }

    private static synchronized void reservations(HttpExchange x) throws IOException {
        try {
            if ("GET".equals(x.getRequestMethod())) {
                StringBuilder out = new StringBuilder("[");
                for (Reservation r : reservations) { if (out.length() > 1) out.append(','); out.append(reservationJson(r)); }
                json(x, 200, out.append(']').toString());
            } else if ("POST".equals(x.getRequestMethod())) {
                Map<String, String> p = form(x); Reservation r = new Reservation(RESERVATION_IDS.getAndIncrement(), required(p, "guestName"), positiveInt(p, "tableNumber"), required(p, "contactNo"), "Active");
                reservations.add(r); json(x, 201, reservationJson(r));
            } else if ("DELETE".equals(x.getRequestMethod())) {
                int id = integer(query(x), "id");
                boolean removed = reservations.removeIf(r -> r.id == id);
                if (removed) {
                    orders.removeIf(o -> o.reservationId == id);
                }
                json(x, removed ? 200 : 404, removed ? "{\"ok\":true}" : error("Reservation not found"));
            } else method(x, "GET");
        } catch (IllegalArgumentException e) { json(x, 400, error(e.getMessage())); }
    }

    private static synchronized void orders(HttpExchange x) throws IOException {
        try {
            if ("GET".equals(x.getRequestMethod())) json(x, 200, ordersJson(integer(query(x), "reservationId")));
            else if ("POST".equals(x.getRequestMethod())) {
                Map<String, String> p = form(x); int rid = integer(p, "reservationId"), iid = integer(p, "itemId"), qty = positiveInt(p, "quantity");
                Reservation r = findReservation(rid); MenuItem i = findItem(iid);
                if (r == null || !"Active".equals(r.status)) { json(x, 400, error("Choose an active reservation")); return; }
                if (i == null) { json(x, 400, error("Choose a valid menu item")); return; }
                Order o = new Order(ORDER_IDS.getAndIncrement(), rid, iid, qty); orders.add(o); json(x, 201, orderJson(o));
            } else if ("DELETE".equals(x.getRequestMethod())) {
                int id = integer(query(x), "id"); boolean ok = orders.removeIf(o -> o.id == id);
                json(x, ok ? 200 : 404, ok ? "{\"ok\":true}" : error("Order not found"));
            } else method(x, "GET");
        } catch (IllegalArgumentException e) { json(x, 400, error(e.getMessage())); }
    }

    private static synchronized void bill(HttpExchange x) throws IOException {
        try {
            Map<String, String> p = "GET".equals(x.getRequestMethod()) ? query(x) : form(x);
            int rid = integer(p, "reservationId"); Reservation r = findReservation(rid);
            if (r == null) { json(x, 404, error("Reservation not found")); return; }
            double subtotal = orders.stream().filter(o -> o.reservationId == rid).mapToDouble(RestaurantWebServer::total).sum();
            if (subtotal == 0) { json(x, 400, error("Add at least one order item first")); return; }
            if ("POST".equals(x.getRequestMethod())) r.status = "Completed";
            double tax = subtotal * .05;
            json(x, 200, "{\"reservation\":" + reservationJson(r) + ",\"orders\":" + ordersJson(rid) + ",\"subtotal\":" + subtotal + ",\"tax\":" + tax + ",\"total\":" + (subtotal + tax) + "}");
        } catch (IllegalArgumentException e) { json(x, 400, error(e.getMessage())); }
    }

    private static String itemJson(MenuItem i) { return "{\"id\":"+i.id+",\"name\":\""+escape(i.name)+"\",\"price\":"+i.price+"}"; }
    private static String reservationJson(Reservation r) { return "{\"id\":"+r.id+",\"guestName\":\""+escape(r.guestName)+"\",\"tableNumber\":"+r.tableNumber+",\"contactNo\":\""+escape(r.contactNo)+"\",\"status\":\""+r.status+"\"}"; }
    private static String orderJson(Order o) { MenuItem i=findItem(o.itemId); return "{\"id\":"+o.id+",\"itemName\":\""+escape(i.name)+"\",\"price\":"+i.price+",\"quantity\":"+o.quantity+",\"total\":"+total(o)+"}"; }
    private static String ordersJson(int rid) { StringBuilder out=new StringBuilder("["); for(Order o:orders) if(o.reservationId==rid){ if(out.length()>1)out.append(','); out.append(orderJson(o)); } return out.append(']').toString(); }

    private static void staticFile(HttpExchange x) throws IOException {
        String requested = "/".equals(x.getRequestURI().getPath()) ? "index.html" : x.getRequestURI().getPath().substring(1);
        Path file = WEB_ROOT.resolve(requested).normalize();
        if (!file.startsWith(WEB_ROOT) || !Files.isRegularFile(file)) { text(x, 404, "Not found", "text/plain"); return; }
        String type = requested.endsWith(".css") ? "text/css" : requested.endsWith(".js") ? "application/javascript" : "text/html";
        byte[] bytes = Files.readAllBytes(file); x.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        x.getResponseHeaders().set("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        x.getResponseHeaders().set("Pragma", "no-cache");
        x.sendResponseHeaders(200, bytes.length); x.getResponseBody().write(bytes); x.close();
    }
    private static Map<String,String> form(HttpExchange x)throws IOException{return parse(new String(x.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));}
    private static Map<String,String> query(HttpExchange x){return parse(x.getRequestURI().getRawQuery());}
    private static Map<String,String> parse(String raw){Map<String,String>p=new LinkedHashMap<>();if(raw==null||raw.isBlank())return p;for(String pair:raw.split("&")){String[]kv=pair.split("=",2);p.put(decode(kv[0]),kv.length>1?decode(kv[1]):"");}return p;}
    private static String decode(String v){return URLDecoder.decode(v,StandardCharsets.UTF_8);}
    private static boolean method(HttpExchange x,String expected)throws IOException{if(expected.equals(x.getRequestMethod()))return true;json(x,405,error("Method not allowed"));return false;}
    private static void json(HttpExchange x,int status,String value)throws IOException{text(x,status,value,"application/json");}
    private static void text(HttpExchange x,int status,String value,String type)throws IOException{byte[]b=value.getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type",type+"; charset=utf-8");x.sendResponseHeaders(status,b.length);x.getResponseBody().write(b);x.close();}
    private static String required(Map<String,String>p,String k){String v=p.getOrDefault(k,"").trim();if(v.isEmpty())throw new IllegalArgumentException(k+" is required");return v;}
    private static int integer(Map<String,String>p,String k){try{return Integer.parseInt(p.getOrDefault(k,"0"));}catch(NumberFormatException e){return 0;}}
    private static int positiveInt(Map<String,String>p,String k){int v=integer(p,k);if(v<1)throw new IllegalArgumentException(k+" must be positive");return v;}
    private static double positiveDouble(Map<String,String>p,String k){double v;try{v=Double.parseDouble(p.getOrDefault(k,"0"));}catch(NumberFormatException e){v=0;}if(v<=0)throw new IllegalArgumentException(k+" must be positive");return v;}
    private static Reservation findReservation(int id){return reservations.stream().filter(r->r.id==id).findFirst().orElse(null);}
    private static MenuItem findItem(int id){return menu.stream().filter(i->i.id==id).findFirst().orElse(null);}
    private static double total(Order o){MenuItem i=findItem(o.itemId);return i==null?0:i.price*o.quantity;}
    private static String escape(String s){return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");}
    private static String error(String s){return "{\"error\":\""+escape(s)+"\"}";}
    private static final class Reservation{int id,tableNumber;String guestName,contactNo,status;Reservation(int i,String g,int t,String c,String s){id=i;guestName=g;tableNumber=t;contactNo=c;status=s;}}
    private static final class MenuItem{int id;String name;double price;MenuItem(int i,String n,double p){id=i;name=n;price=p;}}
    private static final class Order{int id,reservationId,itemId,quantity;Order(int i,int r,int m,int q){id=i;reservationId=r;itemId=m;quantity=q;}}
}
