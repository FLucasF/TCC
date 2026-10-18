package br.com.loja.pedidos.order;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import br.com.loja.pedidos.checkout.Summary;

public record Order(
        String id,
        OrderStatus status,
        Instant createdAt,
        Summary summary,
        List<OrderItem> items,
        String trackingCode,
        @JsonIgnore String cpf,
        @JsonIgnore String email) {
}
