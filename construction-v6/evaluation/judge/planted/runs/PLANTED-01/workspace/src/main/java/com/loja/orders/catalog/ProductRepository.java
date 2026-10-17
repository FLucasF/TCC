package com.loja.orders.catalog;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.loja.orders.common.Money;

@Repository
public class ProductRepository {

    private final JdbcClient jdbc;

    public ProductRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Product> findBySku(String sku) {
        return jdbc.sql("SELECT * FROM product WHERE sku = :sku")
                .param("sku", sku)
                .query(ProductRepository::map)
                .optional();
    }

    public Map<String, Product> findBySkus(Collection<String> skus) {
        if (skus.isEmpty()) {
            return Map.of();
        }
        return jdbc.sql("SELECT * FROM product WHERE sku IN (:skus)")
                .param("skus", skus)
                .query(ProductRepository::map)
                .list()
                .stream()
                .collect(Collectors.toMap(Product::sku, Function.identity()));
    }

    /** Lê o estoque travando as linhas até o fim da transação, sempre na ordem do SKU. */
    public Map<String, Integer> lockStock(Collection<String> skus) {
        return jdbc.sql("SELECT sku, stock FROM product WHERE sku IN (:skus) ORDER BY sku FOR UPDATE")
                .param("skus", skus)
                .query((rs, n) -> Map.entry(rs.getString("sku"), rs.getInt("stock")))
                .list()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void changeStock(String sku, int delta) {
        jdbc.sql("UPDATE product SET stock = stock + :delta WHERE sku = :sku")
                .param("delta", delta)
                .param("sku", sku)
                .update();
    }


    /** Peso com pelo menos 2 casas (0.30), sem perder gramas quebradas. */
    private static BigDecimal weight(BigDecimal kg) {
        BigDecimal stripped = kg.stripTrailingZeros();
        return stripped.scale() < 2 ? stripped.setScale(2) : stripped;
    }

    private static Product map(ResultSet rs, int rowNum) throws SQLException {
        return new Product(
                rs.getString("sku"),
                rs.getString("name"),
                Money.round(rs.getBigDecimal("price")),
                weight(rs.getBigDecimal("weight_kg")),
                rs.getInt("stock"),
                rs.getBoolean("discontinued"));
    }
}
