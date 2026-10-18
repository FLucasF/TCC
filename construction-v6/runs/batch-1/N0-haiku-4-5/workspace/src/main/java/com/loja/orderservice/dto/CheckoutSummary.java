package com.loja.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutSummary {
    @JsonProperty("productsSubtotal")
    private BigDecimal productsSubtotal;

    @JsonProperty("couponDiscount")
    private BigDecimal couponDiscount;

    @JsonProperty("shippingCost")
    private BigDecimal shippingCost;

    @JsonProperty("deliveryDays")
    private Integer deliveryDays;

    @JsonProperty("insurance")
    private BigDecimal insurance;

    @JsonProperty("paymentAdjustment")
    private BigDecimal paymentAdjustment;

    @JsonProperty("finalTotal")
    private BigDecimal finalTotal;

    @JsonProperty("installments")
    private Integer installments;

    @JsonProperty("installmentAmount")
    private BigDecimal installmentAmount;

    @JsonProperty("nextPurchaseCredit")
    private BigDecimal nextPurchaseCredit;

    @JsonProperty("gift")
    private Boolean gift;
}
