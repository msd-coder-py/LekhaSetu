package inventory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApiServer {

    private static final Inventory inventory = Inventory.load();
    private static final DateTimeFormatter DT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static void main(String[] args) throws Exception {
       int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
       HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        server.createContext("/api/products", ApiServer::handleProducts);
        server.createContext("/api/dashboard", ApiServer::handleDashboard);
        server.createContext("/api/transactions", ApiServer::handleTransactions);
        server.createContext("/api/stock", ApiServer::handleStock);

        server.setExecutor(null);

        System.out.println("=================================");
        System.out.println("   LEKHASETU JAVA API SERVER");
        System.out.println("=================================");
        System.out.println("Server running at:");
        System.out.println("http://localhost:8080");
        System.out.println();

        server.start();
    }

    private static void handleProducts(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        if (method.equals("OPTIONS")) {
            sendResponse(exchange, 204, "");
            return;
        }

        try {
            if (method.equals("GET")) {
                sendResponse(exchange, 200, productsJson());
                return;
            }

            Map<String, String> q = queryParams(exchange.getRequestURI());
            if (method.equals("POST")) {
                Product p = new Product(
                        required(q, "sku"),
                        required(q, "name"),
                        q.getOrDefault("category", "General"),
                        q.getOrDefault("unit", "Piece"),
                        number(q, "price"),
                        integer(q, "quantity"),
                        integer(q, "reorderLevel"),
                        q.getOrDefault("supplier", "-")
                );
                inventory.addProduct(p);
                inventory.save();
                sendResponse(exchange, 201, productJson(p));
                return;
            }

            if (method.equals("PUT")) {
                Product p = inventory.find(required(q, "sku"));
                p.setName(required(q, "name"));
                p.setCategory(q.getOrDefault("category", "General"));
                p.setUnit(q.getOrDefault("unit", "Piece"));
                p.setPrice(number(q, "price"));
                int oldQty = p.getQuantity();
                int newQty = integer(q, "quantity");
                p.setReorderLevel(integer(q, "reorderLevel"));
                p.setSupplier(q.getOrDefault("supplier", "-"));
                if (newQty > oldQty) inventory.purchase(p.getSku(), newQty - oldQty, p.getPrice());
                else if (newQty < oldQty) inventory.sell(p.getSku(), oldQty - newQty);
                inventory.save();
                sendResponse(exchange, 200, productJson(p));
                return;
            }

            if (method.equals("DELETE")) {
                String sku = required(q, "sku");
                Product p = inventory.find(sku);
                inventory.deleteProduct(sku);
                inventory.save();
                sendResponse(exchange, 200, productJson(p));
                return;
            }

            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, errorJson(e.getMessage()));
        } catch (Exception e) {
            sendResponse(exchange, 500, errorJson(e.getMessage()));
        }
    }

    private static void handleStock(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        if (method.equals("OPTIONS")) {
            sendResponse(exchange, 204, "");
            return;
        }
        if (!method.equals("POST")) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        try {
            Map<String, String> q = queryParams(exchange.getRequestURI());
            Product p = inventory.find(required(q, "sku"));
            int delta = integer(q, "delta");
            int oldQty = p.getQuantity();

            if (delta > 0) {
                inventory.purchase(p.getSku(), delta, p.getPrice());
            } else if (delta < 0) {
                inventory.sell(p.getSku(), -delta);
            } else {
                throw new IllegalArgumentException("Stock adjustment cannot be zero");
            }

            inventory.save();
            sendResponse(exchange, 200, "{\"product\":" + productJson(p)
                    + ",\"oldQuantity\":" + oldQty
                    + ",\"newQuantity\":" + p.getQuantity() + "}");
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, errorJson(e.getMessage()));
        } catch (Exception e) {
            sendResponse(exchange, 500, errorJson(e.getMessage()));
        }
    }

    private static void handleDashboard(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        int totalProducts = inventory.getProducts().size();
        int lowStock = inventory.lowStock().size();
        int totalUnits = inventory.getProducts().stream().mapToInt(Product::getQuantity).sum();
        double totalValue = inventory.totalValue();

        String json = "{" +
                "\"totalProducts\":" + totalProducts + "," +
                "\"totalUnits\":" + totalUnits + "," +
                "\"lowStock\":" + lowStock + "," +
                "\"totalValue\":" + totalValue +
                "}";
        sendResponse(exchange, 200, json);
    }

    private static void handleTransactions(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (Transaction t : inventory.getTransactions()) {
            if (!first) json.append(",");
            json.append("{")
                    .append("\"id\":\"").append(escape(t.getTime().toString())).append("\",")
                    .append("\"sku\":\"").append(escape(t.getSku())).append("\",")
                    .append("\"productName\":\"").append(escape(t.getProductName())).append("\",")
                    .append("\"type\":\"").append(escape(t.getType().toString())).append("\",")
                    .append("\"quantity\":").append(t.getQuantity()).append(",")
                    .append("\"delta\":").append(t.getType() == Transaction.Type.PURCHASE ? t.getQuantity() : -t.getQuantity()).append(",")
                    .append("\"amount\":").append(t.getAmount()).append(",")
                    .append("\"timestamp\":\"").append(escape(t.getTime().format(DT))).append("\",")
                    .append("\"user\":\"java_backend\",")
                    .append("\"note\":\"").append(escape(t.getType() == Transaction.Type.PURCHASE ? "Purchase" : "Sale")).append("\"")
                    .append("}");
            first = false;
        }
        json.append("]");
        sendResponse(exchange, 200, json.toString());
    }

    private static String productsJson() {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (Product p : inventory.getProducts()) {
            if (!first) json.append(",");
            json.append(productJson(p));
            first = false;
        }
        return json.append("]").toString();
    }

    private static String productJson(Product p) {
        return "{" +
                "\"sku\":\"" + escape(p.getSku()) + "\"," +
                "\"name\":\"" + escape(p.getName()) + "\"," +
                "\"category\":\"" + escape(p.getCategory()) + "\"," +
                "\"unit\":\"" + escape(p.getUnit()) + "\"," +
                "\"price\":" + p.getPrice() + "," +
                "\"quantity\":" + p.getQuantity() + "," +
                "\"reorderLevel\":" + p.getReorderLevel() + "," +
                "\"supplier\":\"" + escape(p.getSupplier()) + "\"," +
                "\"status\":\"" + escape(p.getStatus()) + "\"" +
                "}";
    }

    private static Map<String, String> queryParams(URI uri) {
        Map<String, String> result = new LinkedHashMap<>();
        String raw = uri.getRawQuery();
        if (raw == null || raw.isEmpty()) return result;
        for (String pair : raw.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length > 1 ? decode(parts[1]) : "";
            result.put(key, value);
        }
        return result;
    }

    private static String required(Map<String, String> q, String key) {
        String value = q.get(key);
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(key + " is required");
        return value;
    }

    private static int integer(Map<String, String> q, String key) {
        try { return Integer.parseInt(required(q, key)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(key + " must be an integer"); }
    }

    private static double number(Map<String, String> q, String key) {
        try { return Double.parseDouble(required(q, key)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(key + " must be a number"); }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String errorJson(String message) {
        return "{\"error\":\"" + escape(message == null ? "Unknown error" : message) + "\"}";
    }

    private static void sendResponse(HttpExchange exchange, int status, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) { output.write(bytes); }
    }

    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
