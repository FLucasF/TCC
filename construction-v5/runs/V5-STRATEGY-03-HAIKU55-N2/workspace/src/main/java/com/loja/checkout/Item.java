package com.loja.checkout;

import java.math.BigDecimal;

public record Item(BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {
}
