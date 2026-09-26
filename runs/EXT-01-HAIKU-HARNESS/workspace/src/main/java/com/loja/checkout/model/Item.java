package com.loja.checkout.model;

import java.math.BigDecimal;

public record Item(
    String nome,
    BigDecimal precoUnitario,
    int quantidade,
    BigDecimal pesoKg
) {}
