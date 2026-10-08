package inventory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/** Business logic. Uses collections: LinkedHashMap (products), ArrayList (history), TreeSet (categories). */
public class Inventory implements Serializable {
    private static final File FILE = new File("stationery_inventory.dat");
    private final Map<String, Product> products = new LinkedHashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();

    public void addProduct(Product p) {
        if (products.containsKey(p.getSku())) throw new IllegalArgumentException("SKU already exists: " + p.getSku());
        products.put(p.getSku(), p);
    }
    public Product find(String sku) {
        Product p = products.get(sku == null ? "" : sku.trim().toUpperCase());
        if (p == null) throw new IllegalArgumentException("Product not found: " + sku);
        return p;
    }
    public void deleteProduct(String sku) { products.remove(find(sku).getSku()); }

    public void purchase(String sku, int qty, double unitCost) {
        Product p = find(sku); p.addStock(qty);
        transactions.add(new Transaction(Transaction.Type.PURCHASE, p, qty, unitCost));
    }
    public void sell(String sku, int qty) {
        Product p = find(sku); p.removeStock(qty);
        transactions.add(new Transaction(Transaction.Type.SALE, p, qty, p.getPrice()));
    }

    public List<Product> getProducts() { return new ArrayList<>(products.values()); }
    public List<Transaction> getTransactions() { List<Transaction> l = new ArrayList<>(transactions); Collections.reverse(l); return l; }
    public List<Product> lowStock() { return products.values().stream().filter(Product::isLowStock).collect(Collectors.toList()); }
    public double totalValue() { return products.values().stream().mapToDouble(p -> p.getPrice() * p.getQuantity()).sum(); }
    public double total(Transaction.Type t) { return transactions.stream().filter(x -> x.getType() == t).mapToDouble(Transaction::getAmount).sum(); }
    public Set<String> categories() { return products.values().stream().map(Product::getCategory).collect(Collectors.toCollection(TreeSet::new)); }

    public void save() {
        try (ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(FILE))) { o.writeObject(this); }
        catch (IOException e) { System.err.println("Could not save: " + e.getMessage()); }
    }
    public static Inventory load() {
        if (FILE.exists()) try (ObjectInputStream i = new ObjectInputStream(new FileInputStream(FILE))) { return (Inventory) i.readObject(); }
        catch (Exception e) { System.err.println("Could not load, starting fresh: " + e.getMessage()); }
        return new Inventory();   // first run: completely empty, user enters all data
    }
}
