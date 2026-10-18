package com.loja.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
  Long id,
  String status,
  @JsonProperty("createdAt")
  Instant createdAt,
  OrderSummaryResponse summary,
  List<OrderItemResponse> items,
  @JsonProperty("trackingCode")
  String trackingCode
) {
  public record OrderSummaryResponse(
    @JsonProperty("productsSubtotal")
    BigDecimal productsSubtotal,
    @JsonProperty("couponDiscount")
    BigDecimal couponDiscount,
    @JsonProperty("shippingCost")
    BigDecimal shippingCost,
    @JsonProperty("deliveryDays")
    int deliveryDays,
    BigDecimal insurance,
    @JsonProperty("paymentAdjustment")
    BigDecimal paymentAdjustment,
    @JsonProperty("finalTotal")
    BigDecimal finalTotal,
    int installments,
    @JsonProperty("installmentAmount")
    BigDecimal installmentAmount,
    @JsonProperty("nextPurchaseCredit")
    BigDecimal nextPurchaseCredit,
    boolean gift
  ) {}

  public record OrderItemResponse(
    String sku,
    Integer quantity
  ) {}
}
