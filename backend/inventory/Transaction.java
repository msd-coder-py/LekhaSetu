package inventory;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Immutable record of one purchase or sale. */
public class Transaction implements Serializable {
    public enum Type { PURCHASE, SALE }
    private final Type type;
    private final String sku, productName;
    private final int quantity;
    private final double amount;
    private final LocalDateTime time = LocalDateTime.now();

    public Transaction(Type type, Product p, int quantity, double unitPrice) {
        this.type = type; this.sku = p.getSku(); this.productName = p.getName();
        this.quantity = quantity; this.amount = quantity * unitPrice;
    }
    public Type getType() { return type; }
    public String getSku() { return sku; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getAmount() { return amount; }
    public LocalDateTime getTime() { return time; }
}
