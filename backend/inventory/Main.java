package inventory;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/** SAP Fiori-inspired Swing UI for the Stationery Inventory Management System. */
public class Main extends JFrame {
    static final Color SHELL = new Color(0x354A5F), BLUE = new Color(0x0A6ED1), BG = new Color(0xF2F4F6), LINE = new Color(0xD9D9D9),
            RED = new Color(0xBB0000), ORANGE = new Color(0xE9730C), GREEN = new Color(0x107E3E), TEXT = new Color(0x32363A);
    static final Font F = new Font("Segoe UI", Font.PLAIN, 13), FB = F.deriveFont(Font.BOLD);
    static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    interface Act { void go() throws Exception; }

    final Inventory inv = Inventory.load();
    final CardLayout cards = new CardLayout();
    final JPanel content = new JPanel(cards);
    final List<Runnable> refreshers = new ArrayList<>();
    final Map<String, JButton> nav = new LinkedHashMap<>();

    Main() {
        super("Stationery Inventory Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(1200, 730); setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(shellBar(), BorderLayout.NORTH); add(sidebar(), BorderLayout.WEST);
        content.setBackground(BG);
        content.add(dashboard(), "Dashboard"); content.add(products(), "Products");
        content.add(txPanel(true), "Purchases"); content.add(txPanel(false), "Sales");
        content.add(alerts(), "Alerts"); content.add(history(), "History");
        add(content, BorderLayout.CENTER);
        refresh(); show("Dashboard");
    }

    // ---------- helpers ----------
    void refresh() { refreshers.forEach(Runnable::run); inv.save(); }
    void act(Act a) {
        try { a.go(); refresh(); }
        catch (NumberFormatException e) { msg("Please enter valid numbers for price, quantity and reorder level.", JOptionPane.ERROR_MESSAGE); }
        catch (Exception e) { msg(e.getMessage(), JOptionPane.ERROR_MESSAGE); }
    }
    void msg(String m, int type) { JOptionPane.showMessageDialog(this, m, "Stationery Inventory", type); }
    static String money(double v) { return String.format("\u20B9%,.2f", v); }
    void show(String name) {
        cards.show(content, name);
        nav.forEach((k, b) -> { boolean on = k.equals(name); b.setBackground(on ? BLUE : Color.WHITE); b.setForeground(on ? Color.WHITE : TEXT); });
    }
    JTextField tf(int c) { JTextField f = new JTextField(c); f.setFont(F); f.setBorder(new CompoundBorder(new LineBorder(LINE), new EmptyBorder(5, 7, 5, 7))); return f; }
    JButton btn(String t, Color c) {
        JButton b = new JButton(t); b.setFont(FB); b.setBackground(c); b.setForeground(Color.WHITE); b.setFocusPainted(false);
        b.setContentAreaFilled(false); b.setOpaque(true); b.setBorder(new EmptyBorder(9, 18, 9, 18)); b.setCursor(new Cursor(Cursor.HAND_CURSOR)); return b;
    }
    JPanel card() { JPanel p = new JPanel(new BorderLayout()); p.setBackground(Color.WHITE); p.setBorder(new CompoundBorder(new LineBorder(LINE), new EmptyBorder(14, 16, 14, 16))); return p; }
    JPanel page(String title, JComponent body) {
        JPanel p = new JPanel(new BorderLayout(0, 14)); p.setBackground(BG); p.setBorder(new EmptyBorder(20, 24, 20, 24));
        JLabel h = new JLabel(title); h.setFont(F.deriveFont(Font.BOLD, 22f)); h.setForeground(TEXT);
        p.add(h, BorderLayout.NORTH); p.add(body, BorderLayout.CENTER); return p;
    }
    JPanel field(String label, JComponent c) {
        JPanel p = new JPanel(new BorderLayout(0, 3)); p.setOpaque(false); JLabel l = new JLabel(label); l.setFont(F); l.setForeground(Color.GRAY);
        c.setFont(F); p.add(l, BorderLayout.NORTH); p.add(c); return p;
    }
    DefaultTableModel model(String... cols) { return new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } }; }
    JTable table(DefaultTableModel m) {
        JTable t = new JTable(m); t.setFont(F); t.setRowHeight(30); t.setGridColor(LINE); t.setShowVerticalLines(false);
        t.setSelectionBackground(new Color(0xE5F0FA)); t.setSelectionForeground(TEXT);
        JTableHeader h = t.getTableHeader(); h.setFont(FB); h.setBackground(new Color(0xEDEFF0)); h.setForeground(TEXT); h.setPreferredSize(new Dimension(0, 34));
        int s = m.findColumn("Status");
        if (s >= 0) t.getColumnModel().getColumn(s).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(tb, v, sel, foc, r, c); String x = String.valueOf(v); setFont(FB);
                setForeground(x.startsWith("OUT") ? RED : x.startsWith("LOW") ? ORANGE : GREEN); return this;
            }
        });
        return t;
    }
    JScrollPane scroll(JTable t) { JScrollPane s = new JScrollPane(t); s.setBorder(new LineBorder(LINE)); s.getViewport().setBackground(Color.WHITE); return s; }

    // ---------- shell ----------
    JComponent shellBar() {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(SHELL); p.setBorder(new EmptyBorder(12, 20, 12, 20));
        JLabel t = new JLabel("Stationery Inventory Management"); t.setForeground(Color.WHITE); t.setFont(F.deriveFont(Font.BOLD, 17f));
        JLabel r = new JLabel("Store Manager  |  Inventory v1.0"); r.setForeground(new Color(0xC7D3DF)); r.setFont(F);
        p.add(t, BorderLayout.WEST); p.add(r, BorderLayout.EAST); return p;
    }
    JComponent sidebar() {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(new MatteBorder(0, 0, 0, 1, LINE), new EmptyBorder(16, 0, 0, 0))); p.setPreferredSize(new Dimension(200, 0));
        for (String n : new String[]{"Dashboard", "Products", "Purchases", "Sales", "Alerts", "History"}) {
            JButton b = new JButton("   " + n); b.setFont(FB); b.setHorizontalAlignment(SwingConstants.LEFT); b.setFocusPainted(false);
            b.setContentAreaFilled(false); b.setOpaque(true); b.setBorder(new EmptyBorder(13, 12, 13, 12));
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46)); b.setAlignmentX(0f);
            b.addActionListener(e -> show(n)); nav.put(n, b); p.add(b);
        }
        return p;
    }

    // ---------- screens ----------
    JComponent dashboard() {
        String[] names = {"Total Products", "Stock Value", "Total Sales", "Low-Stock Items"};
        Color[] cols = {BLUE, GREEN, BLUE, ORANGE}; JLabel[] k = new JLabel[4];
        JPanel tiles = new JPanel(new GridLayout(1, 4, 16, 0)); tiles.setOpaque(false);
        for (int i = 0; i < 4; i++) {
            JPanel c = card(); JLabel n = new JLabel(names[i]); n.setForeground(Color.GRAY); n.setFont(F);
            k[i] = new JLabel("0"); k[i].setFont(F.deriveFont(Font.BOLD, 28f)); k[i].setForeground(cols[i]);
            c.add(n, BorderLayout.NORTH); c.add(k[i]); tiles.add(c);
        }
        JLabel note = new JLabel(); note.setFont(F.deriveFont(14f)); note.setVerticalAlignment(SwingConstants.TOP);
        JPanel nc = card(); nc.add(note);
        JPanel body = new JPanel(new BorderLayout(0, 16)); body.setOpaque(false); body.add(tiles, BorderLayout.NORTH); body.add(nc);
        refreshers.add(() -> {
            k[0].setText("" + inv.getProducts().size()); k[1].setText(money(inv.totalValue()));
            k[2].setText(money(inv.total(Transaction.Type.SALE))); k[3].setText("" + inv.lowStock().size());
            note.setText(inv.getProducts().isEmpty()
                ? "<html><h2>Welcome!</h2>Your inventory is empty. Open <b>Products</b> and enter all your stationery items first "
                  + "(code, name, category, price, opening stock, reorder level). You can edit or correct them any time later.</html>"
                : inv.lowStock().isEmpty() ? "<html><h3 style='color:green'>All products are sufficiently stocked.</h3></html>"
                : "<html><h3 style='color:#E9730C'>" + inv.lowStock().size() + " item(s) need restocking. Open <b>Alerts</b> to review.</h3></html>");
        });
        return page("Overview", body);
    }

    JComponent products() {
        JTextField sku = tf(8), name = tf(12), price = tf(6), qty = tf(6), reorder = tf(6), supplier = tf(10), search = tf(14);
        JComboBox<String> cat = new JComboBox<>(new String[]{"Pens & Pencils", "Notebooks & Paper", "Files & Folders", "Art & Craft", "Desk Accessories", "Adhesives & Tapes", "Office Supplies"});
        cat.setEditable(true);
        JComboBox<String> unit = new JComboBox<>(new String[]{"Piece", "Pack", "Box", "Dozen", "Ream", "Set"});
        DefaultTableModel m = model("SKU", "Product", "Category", "Unit", "Price", "Qty", "Reorder Lvl", "Supplier", "Status"); JTable t = table(m);
        Runnable clear = () -> { for (JTextField f : new JTextField[]{sku, name, price, qty, reorder, supplier}) f.setText(""); sku.setEnabled(true); t.clearSelection(); };

        JPanel form = new JPanel(new GridLayout(2, 4, 12, 8)); form.setOpaque(false);
        form.add(field("Product Code (SKU)", sku)); form.add(field("Product Name", name)); form.add(field("Category", cat)); form.add(field("Unit", unit));
        form.add(field("Unit Price (\u20B9)", price)); form.add(field("Stock Quantity", qty)); form.add(field("Reorder Level", reorder)); form.add(field("Supplier", supplier));
        JButton add = btn("Add Product", BLUE), upd = btn("Update Selected", GREEN), del = btn("Delete", RED), clr = btn("Clear", Color.GRAY);
        add.addActionListener(e -> act(() -> { inv.addProduct(new Product(sku.getText(), name.getText(), (String) cat.getSelectedItem(), (String) unit.getSelectedItem(),
                Double.parseDouble(price.getText().trim()), Integer.parseInt(qty.getText().trim()), Integer.parseInt(reorder.getText().trim()), supplier.getText())); clear.run(); }));
        upd.addActionListener(e -> act(() -> {
            if (sku.isEnabled()) throw new IllegalStateException("Select a product from the table to update it.");
            double pr = Double.parseDouble(price.getText().trim()); int q = Integer.parseInt(qty.getText().trim()), r = Integer.parseInt(reorder.getText().trim());
            Product p = inv.find(sku.getText()); p.setName(name.getText()); p.setCategory((String) cat.getSelectedItem()); p.setUnit((String) unit.getSelectedItem());
            p.setPrice(pr); p.setQuantity(q); p.setReorderLevel(r); p.setSupplier(supplier.getText()); clear.run(); }));
        del.addActionListener(e -> act(() -> {
            if (sku.isEnabled()) throw new IllegalStateException("Select a product from the table to delete it.");
            if (JOptionPane.showConfirmDialog(this, "Delete " + sku.getText() + "?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) { inv.deleteProduct(sku.getText()); clear.run(); } }));
        clr.addActionListener(e -> clear.run());
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); bar.setOpaque(false); bar.add(add); bar.add(upd); bar.add(del); bar.add(clr);
        JPanel top = new JPanel(new BorderLayout(0, 12)); top.setOpaque(false); top.add(form); top.add(bar, BorderLayout.SOUTH);
        JPanel fc = card(); fc.add(top);

        t.getSelectionModel().addListSelectionListener(e -> {
            int r = t.getSelectedRow(); if (r < 0 || e.getValueIsAdjusting()) return;
            Product p = inv.find((String) m.getValueAt(r, 0));
            sku.setText(p.getSku()); sku.setEnabled(false); name.setText(p.getName()); cat.setSelectedItem(p.getCategory()); unit.setSelectedItem(p.getUnit());
            price.setText("" + p.getPrice()); qty.setText("" + p.getQuantity()); reorder.setText("" + p.getReorderLevel()); supplier.setText(p.getSupplier());
        });
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refresh(); } public void removeUpdate(javax.swing.event.DocumentEvent e) { refresh(); } public void changedUpdate(javax.swing.event.DocumentEvent e) { }
        });
        refreshers.add(() -> {
            m.setRowCount(0); String q = search.getText().toLowerCase();
            for (Product p : inv.getProducts()) if ((p.getSku() + p.getName() + p.getCategory()).toLowerCase().contains(q))
                m.addRow(new Object[]{p.getSku(), p.getName(), p.getCategory(), p.getUnit(), money(p.getPrice()), p.getQuantity(), p.getReorderLevel(), p.getSupplier(), p.getStatus()});
        });
        JPanel list = new JPanel(new BorderLayout(0, 8)); list.setOpaque(false); list.add(field("Search", search), BorderLayout.NORTH); list.add(scroll(t));
        JPanel body = new JPanel(new BorderLayout(0, 14)); body.setOpaque(false); body.add(fc, BorderLayout.NORTH); body.add(list);
        return page("Product Master Data", body);
    }

    JComponent txPanel(boolean buy) {
        Transaction.Type type = buy ? Transaction.Type.PURCHASE : Transaction.Type.SALE;
        JComboBox<String> prod = new JComboBox<>(); JSpinner qty = new JSpinner(new SpinnerNumberModel(1, 1, 1000000, 1)); JTextField cost = tf(8); cost.setText("0");
        DefaultTableModel m = model("Date", "SKU", "Product", "Qty", "Amount");
        JButton go = btn(buy ? "Post Purchase" : "Post Sale", buy ? BLUE : GREEN);
        go.addActionListener(e -> act(() -> {
            if (prod.getSelectedItem() == null) throw new IllegalStateException("Add a product first (Products screen).");
            String sku = ((String) prod.getSelectedItem()).split(" \\| ")[0]; int q = (Integer) qty.getValue();
            if (buy) inv.purchase(sku, q, Double.parseDouble(cost.getText().trim()));
            else { inv.sell(sku, q); Product p = inv.find(sku); if (p.isLowStock()) msg("LOW STOCK ALERT: " + p.getName() + " has only " + p.getQuantity() + " left (reorder level " + p.getReorderLevel() + ").", JOptionPane.WARNING_MESSAGE); }
        }));
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0)); form.setOpaque(false);
        prod.setPreferredSize(new Dimension(340, 32)); qty.setPreferredSize(new Dimension(90, 32));
        form.add(field("Product", prod)); form.add(field("Quantity", qty)); if (buy) form.add(field("Cost / Unit (\u20B9)", cost));
        JPanel gb = new JPanel(new BorderLayout()); gb.setOpaque(false); gb.add(go, BorderLayout.SOUTH); form.add(gb);
        JPanel fc = card(); fc.add(form);
        refreshers.add(() -> {
            int idx = prod.getSelectedIndex(); prod.removeAllItems();
            for (Product p : inv.getProducts()) prod.addItem(p.getSku() + " | " + p.getName() + "  (stock " + p.getQuantity() + ")");
            if (prod.getItemCount() > 0) prod.setSelectedIndex(Math.max(0, Math.min(idx, prod.getItemCount() - 1)));
            m.setRowCount(0);
            for (Transaction x : inv.getTransactions()) if (x.getType() == type) m.addRow(new Object[]{x.getTime().format(DT), x.getSku(), x.getProductName(), x.getQuantity(), money(x.getAmount())});
        });
        JPanel body = new JPanel(new BorderLayout(0, 14)); body.setOpaque(false); body.add(fc, BorderLayout.NORTH); body.add(scroll(table(m)));
        return page(buy ? "Purchases (Goods Receipt)" : "Sales (Goods Issue)", body);
    }

    JComponent alerts() {
        DefaultTableModel m = model("SKU", "Product", "Supplier", "Stock", "Reorder Lvl", "Status");
        refreshers.add(() -> {
            m.setRowCount(0);
            for (Product p : inv.lowStock()) m.addRow(new Object[]{p.getSku(), p.getName(), p.getSupplier(), p.getQuantity(), p.getReorderLevel(), p.getStatus()});
            nav.get("Alerts").setText("   Alerts (" + inv.lowStock().size() + ")");
        });
        return page("Low-Stock Alerts", scroll(table(m)));
    }

    JComponent history() {
        DefaultTableModel m = model("Date", "Type", "SKU", "Product", "Qty", "Amount");
        refreshers.add(() -> { m.setRowCount(0);
            for (Transaction x : inv.getTransactions()) m.addRow(new Object[]{x.getTime().format(DT), x.getType(), x.getSku(), x.getProductName(), x.getQuantity(), money(x.getAmount())}); });
        return page("Transaction History", scroll(table(m)));
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
