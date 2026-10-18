package com.loja.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ProductResponse(
  String sku,
  String name,
  BigDecimal price,
  @JsonProperty("weightKg")
  BigDecimal weightKg,
  int stock,
  boolean discontinued
) {}
