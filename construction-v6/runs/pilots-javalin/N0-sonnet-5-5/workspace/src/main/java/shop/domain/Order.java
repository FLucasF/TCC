package shop.domain;

import java.time.Instant;
import java.util.List;

public record Order(String id, String cpf, String email, OrderStatus status, Instant createdAt,
                    Summary summary, List<OrderItem> items, String trackingCode) {
}
