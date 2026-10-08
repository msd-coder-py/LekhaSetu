package inventory;

import java.io.Serializable;

/** A stationery item. All fields are private (ENCAPSULATION); data changes only through validated methods. */
public class Product implements Serializable {
    private final String sku;
    private String name, category, unit, supplier;
    private double price;
    private int quantity, reorderLevel;

    public Product(String sku, String name, String category, String unit,
                   double price, int quantity, int reorderLevel, String supplier) {
        if (sku == null || sku.trim().isEmpty()) throw new IllegalArgumentException("Product code (SKU) is required");
        this.sku = sku.trim().toUpperCase();
        setName(name); setCategory(category); setUnit(unit); setPrice(price);
        setQuantity(quantity); setReorderLevel(reorderLevel); setSupplier(supplier);
    }

    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getUnit() { return unit; }
    public String getSupplier() { return supplier; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public int getReorderLevel() { return reorderLevel; }

    public void setName(String n) { if (n == null || n.trim().isEmpty()) throw new IllegalArgumentException("Product name is required"); name = n.trim(); }
    public void setCategory(String c) { category = (c == null || c.trim().isEmpty()) ? "General" : c.trim(); }
    public void setUnit(String u) { unit = (u == null || u.trim().isEmpty()) ? "Piece" : u.trim(); }
    public void setSupplier(String s) { supplier = (s == null || s.trim().isEmpty()) ? "-" : s.trim(); }
    public void setPrice(double p) { if (p < 0) throw new IllegalArgumentException("Price cannot be negative"); price = p; }
    public void setQuantity(int q) { if (q < 0) throw new IllegalArgumentException("Quantity cannot be negative"); quantity = q; }
    public void setReorderLevel(int r) { if (r < 0) throw new IllegalArgumentException("Reorder level cannot be negative"); reorderLevel = r; }

    public void addStock(int n) {
        if (n <= 0) throw new IllegalArgumentException("Quantity must be greater than 0");
        quantity += n;
    }
    public void removeStock(int n) {
        if (n <= 0) throw new IllegalArgumentException("Quantity must be greater than 0");
        if (n > quantity) throw new IllegalArgumentException("Insufficient stock for " + name + ": only " + quantity + " available");
        quantity -= n;
    }
    public boolean isLowStock() { return quantity <= reorderLevel; }
    public String getStatus() { return quantity == 0 ? "OUT OF STOCK" : isLowStock() ? "LOW STOCK" : "IN STOCK"; }
}
