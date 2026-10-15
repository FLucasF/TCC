package com.loja.roupas.checkout;

import java.math.BigDecimal;

public record ItemCarrinho(
    String nome,
    BigDecimal precoUnitario,
    Integer quantidade,
    BigDecimal pesoKg
) {}
