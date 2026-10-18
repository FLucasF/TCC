package br.com.loja.order;

import java.time.Instant;
import java.util.List;

import br.com.loja.checkout.OrderSummary;

public record Order(String id, String customerCpf, OrderStatus status, Instant createdAt, OrderSummary summary,
                    List<OrderItem> items, String trackingCode, String paymentTransactionId,
                    String paymentOutcome) {
}
