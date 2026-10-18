package shop.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import shop.checkout.Summary;

import java.time.Instant;
import java.util.List;

public record Order(
        String id,
        @JsonIgnore String customerCpf,
        @JsonIgnore String email,
        OrderStatus status,
        Instant createdAt,
        Summary summary,
        List<OrderItem> items,
        String trackingCode) {

    public Order withStatus(OrderStatus newStatus) {
        return new Order(id, customerCpf, email, newStatus, createdAt, summary, items, trackingCode);
    }

    public Order withTrackingCode(String code) {
        return new Order(id, customerCpf, email, status, createdAt, summary, items, code);
    }
}
