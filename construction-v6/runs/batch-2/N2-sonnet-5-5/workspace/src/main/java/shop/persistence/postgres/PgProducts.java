package shop.persistence.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import shop.domain.CartItem;
import shop.domain.Product;
import shop.persistence.ProductRepository;

class PgProducts implements ProductRepository {
    private final Connection c;

    PgProducts(Connection c) {
        this.c = c;
    }

    @Override
    public Optional<Product> find(String sku) {
        return Jdbc.call(() -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT sku, name, price, weight_kg, stock, discontinued FROM products WHERE sku = ?")) {
                ps.setString(1, sku);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(new Product(rs.getString("sku"), rs.getString("name"),
                            rs.getBigDecimal("price"), rs.getBigDecimal("weight_kg"), rs.getInt("stock"),
                            rs.getBoolean("discontinued")));
                }
            }
        });
    }

    @Override
    public void insertIfAbsent(Product p) {
        Jdbc.call(() -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO products(sku, name, price, weight_kg, stock, discontinued) "
                            + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (sku) DO NOTHING")) {
                ps.setString(1, p.sku());
                ps.setString(2, p.name());
                ps.setBigDecimal(3, p.price());
                ps.setBigDecimal(4, p.weightKg());
                ps.setInt(5, p.stock());
                ps.setBoolean(6, p.discontinued());
                ps.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public boolean tryDebit(List<CartItem> items) {
        return Jdbc.call(() -> {
            // Trava as linhas em ordem de SKU: pagamentos simultâneos não se travam um ao outro.
            String[] skus = items.stream().map(CartItem::sku).sorted().toArray(String[]::new);
            Map<String, Integer> stock = new HashMap<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT sku, stock FROM products WHERE sku = ANY(?) ORDER BY sku FOR UPDATE")) {
                ps.setArray(1, c.createArrayOf("text", skus));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        stock.put(rs.getString("sku"), rs.getInt("stock"));
                    }
                }
            }
            for (CartItem item : items) {
                Integer available = stock.get(item.sku());
                if (available == null || available < item.quantity()) {
                    return false;
                }
            }
            adjust(items, -1);
            return true;
        });
    }

    @Override
    public void restore(List<CartItem> items) {
        Jdbc.call(() -> {
            adjust(items, 1);
            return null;
        });
    }

    private void adjust(List<CartItem> items, int sign) throws java.sql.SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE products SET stock = stock + ? WHERE sku = ?")) {
            for (CartItem item : items) {
                ps.setInt(1, sign * item.quantity());
                ps.setString(2, item.sku());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
