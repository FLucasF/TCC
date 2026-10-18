package br.com.loja.pedidos.catalog;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {

    private static final String COLUMNS = "sku, name, price, weight_kg, stock, discontinued";

    private final JdbcClient jdbc;

    public ProductRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Product> find(String sku) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM products WHERE sku = :sku")
                .param("sku", sku).query(this::map).optional();
    }

    public List<Product> findAll(Collection<String> skus) {
        if (skus.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("SELECT " + COLUMNS + " FROM products WHERE sku IN (:skus)")
                .param("skus", skus).query(this::map).list();
    }

    /** Locks the product row until the end of the transaction and returns its current stock. */
    public int lockStock(String sku) {
        return jdbc.sql("SELECT stock FROM products WHERE sku = :sku FOR UPDATE")
                .param("sku", sku).query(Integer.class).single();
    }

    public void decrementStock(String sku, int quantity) {
        int updated = jdbc.sql("UPDATE products SET stock = stock - :q WHERE sku = :sku AND stock >= :q")
                .param("q", quantity).param("sku", sku).update();
        if (updated != 1) {
            throw new IllegalStateException("stock would become negative for " + sku);
        }
    }

    public void incrementStock(String sku, int quantity) {
        jdbc.sql("UPDATE products SET stock = stock + :q WHERE sku = :sku")
                .param("q", quantity).param("sku", sku).update();
    }

    private Product map(ResultSet rs, int row) throws SQLException {
        BigDecimal weight = rs.getBigDecimal("weight_kg").stripTrailingZeros();
        if (weight.scale() < 2) {
            weight = weight.setScale(2);
        }
        return new Product(rs.getString("sku"), rs.getString("name"), rs.getBigDecimal("price").setScale(2),
                weight, rs.getInt("stock"), rs.getBoolean("discontinued"));
    }
}
