package br.com.loja.catalog;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import br.com.loja.order.OrderItem;

@Repository
public class ProductRepository {

    private final JdbcClient jdbc;

    ProductRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Product> findBySku(String sku) {
        return jdbc.sql("SELECT sku, name, price, weight_kg, stock, discontinued FROM products WHERE sku = :sku")
                .param("sku", sku)
                .query((rs, n) -> new Product(rs.getString("sku"), rs.getString("name"),
                        rs.getBigDecimal("price").setScale(2), rs.getBigDecimal("weight_kg").setScale(2),
                        rs.getInt("stock"), rs.getBoolean("discontinued")))
                .optional();
    }

    public List<Product> findAllBySkus(Collection<String> skus) {
        return skus.stream().map(this::findBySku).flatMap(Optional::stream).toList();
    }

    /**
     * Baixa o estoque de todos os itens ou de nenhum. Trava as linhas em ordem de SKU (evita deadlock entre
     * duas máquinas) e só baixa se todas têm saldo; o UPDATE ainda protege com "stock >= quantidade".
     * Precisa rodar dentro de uma transação.
     */
    public boolean reserve(List<OrderItem> items) {
        List<OrderItem> sorted = items.stream().sorted(Comparator.comparing(OrderItem::sku)).toList();
        for (OrderItem item : sorted) {
            Integer stock = jdbc.sql("SELECT stock FROM products WHERE sku = :sku FOR UPDATE")
                    .param("sku", item.sku()).query(Integer.class).single();
            if (stock < item.quantity()) {
                return false;
            }
        }
        for (OrderItem item : sorted) {
            int updated = jdbc.sql("UPDATE products SET stock = stock - :q WHERE sku = :sku AND stock >= :q")
                    .param("q", item.quantity()).param("sku", item.sku()).update();
            if (updated != 1) {
                throw new IllegalStateException("Estoque mudou durante a reserva de " + item.sku());
            }
        }
        return true;
    }

    public void release(List<OrderItem> items) {
        for (OrderItem item : items.stream().sorted(Comparator.comparing(OrderItem::sku)).toList()) {
            jdbc.sql("UPDATE products SET stock = stock + :q WHERE sku = :sku")
                    .param("q", item.quantity()).param("sku", item.sku()).update();
        }
    }
}
