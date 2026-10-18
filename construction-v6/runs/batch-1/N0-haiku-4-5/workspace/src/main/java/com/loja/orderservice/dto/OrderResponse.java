package com.loja.orderservice.dto;

import com.loja.orderservice.domain.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private String id;
    private OrderStatus status;
    private Instant createdAt;
    private CheckoutSummary summary;
    private List<OrderItemResponse> items;
    private String trackingCode;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemResponse {
        private String sku;
        private Long quantity;
    }
}
