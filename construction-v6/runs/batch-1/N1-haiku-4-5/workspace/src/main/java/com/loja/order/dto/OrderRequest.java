package com.loja.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record OrderRequest(
  CustomerRequest customer,
  List<ItemRequest> items,
  @JsonProperty("zipCode")
  String zipCode,
  String region,
  @JsonProperty("shippingMethod")
  String shippingMethod,
  String coupon,
  @JsonProperty("paymentMethod")
  String paymentMethod,
  Integer installments,
  @JsonProperty("clubTier")
  String clubTier
) {
  public record CustomerRequest(
    String cpf,
    String email
  ) {}

  public record ItemRequest(
    String sku,
    Integer quantity
  ) {}
}
