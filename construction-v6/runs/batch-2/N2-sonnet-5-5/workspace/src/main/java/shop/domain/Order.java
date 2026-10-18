package shop.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;

public record Order(String id, OrderStatus status, Instant createdAt, Summary summary, List<CartItem> items,
                    String trackingCode, @JsonIgnore String cpf) {
    public Order withStatus(OrderStatus newStatus) {
        return new Order(id, newStatus, createdAt, summary, items, trackingCode, cpf);
    }

    public Order withShipment(OrderStatus newStatus, String code) {
        return new Order(id, newStatus, createdAt, summary, items, code, cpf);
    }
}
