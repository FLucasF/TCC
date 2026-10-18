package br.com.loja.pedidos.order;

import br.com.loja.pedidos.checkout.OrderSummary;
import java.time.Instant;
import java.util.List;

public record Order(String id, OrderStatus status, Instant createdAt, OrderSummary summary, List<Item> items,
                    String trackingCode) {

    public record Item(String sku, int quantity) {
    }
}
