package com.clothing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CheckoutRequest(
    Customer customer,
    List<OrderItem> items,
    @JsonProperty("zipCode")
    String zipCode,
    Region region,
    @JsonProperty("shippingMethod")
    ShippingMethod shippingMethod,
    String coupon,
    @JsonProperty("paymentMethod")
    PaymentMethod paymentMethod,
    Integer installments,
    @JsonProperty("clubTier")
    ClubTier clubTier
) {
    public int getInstallmentsOrDefault() {
        return installments != null ? installments : 1;
    }
}
