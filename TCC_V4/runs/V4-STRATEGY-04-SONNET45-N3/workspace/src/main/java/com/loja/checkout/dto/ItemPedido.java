package com.loja.checkout.dto;

import java.math.BigDecimal;

public record ItemPedido(
    String nome,
    BigDecimal precoUnitario,
    Integer quantidade,
    BigDecimal pesoKg
) {}
