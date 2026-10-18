package br.com.loja.pedidos.catalog;

import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {

    private final JdbcClient jdbc;

    public ProductRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Product> find(String sku) {
        return jdbc.sql("SELECT sku, name, price, weight_kg, stock, discontinued FROM products WHERE sku = ?")
                .param(sku)
                .query((rs, n) -> new Product(rs.getString("sku"), rs.getString("name"), rs.getBigDecimal("price"),
                        rs.getBigDecimal("weight_kg"), rs.getInt("stock"), rs.getBoolean("discontinued")))
                .optional();
    }

    /** Trava a linha do produto até o fim da transação e devolve o estoque atual. */
    public Optional<Integer> lockStock(String sku) {
        return jdbc.sql("SELECT stock FROM products WHERE sku = ? FOR UPDATE")
                .param(sku)
                .query(Integer.class)
                .optional();
    }

    public void addStock(String sku, int delta) {
        jdbc.sql("UPDATE products SET stock = stock + ? WHERE sku = ?").param(delta).param(sku).update();
    }
}
