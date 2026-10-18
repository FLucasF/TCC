package com.loja.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record SummaryResponse(
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
