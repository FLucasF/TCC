package com.loja.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record CustomerResponse(
  String cpf,
  @JsonProperty("creditBalance")
  BigDecimal creditBalance
) {}
