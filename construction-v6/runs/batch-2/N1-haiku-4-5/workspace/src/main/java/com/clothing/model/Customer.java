package com.clothing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record Customer(
    String cpf,
    String email,
    @JsonProperty("creditBalance")
    BigDecimal creditBalance
) {}
