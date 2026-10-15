package com.loja.checkout.domain;

import java.math.BigDecimal;

public record ItemPedido(
    String nome,
    BigDecimal precoUnitario,
    int quantidade,
    BigDecimal pesoKg
) {
}
