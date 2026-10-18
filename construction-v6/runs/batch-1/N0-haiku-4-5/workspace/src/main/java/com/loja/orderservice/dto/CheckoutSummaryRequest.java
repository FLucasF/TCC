package com.loja.orderservice.dto;

import com.loja.orderservice.domain.enums.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class CheckoutSummaryRequest {
    private CustomerInfo customer;
    private List<OrderItemRequest> items;
    private String zipCode;
    private Region region;
    private ShippingMethod shippingMethod;
    private String coupon;
    private PaymentMethod paymentMethod;
    private Integer installments;
    private ClubTier clubTier;

    @Data
    @NoArgsConstructor
    public static class CustomerInfo {
        private String cpf;
        private String email;
    }

    @Data
    @NoArgsConstructor
    public static class OrderItemRequest {
        private String sku;
        private Long quantity;
    }
}
