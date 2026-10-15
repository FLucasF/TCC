package com.loja.checkout.strategy;

import java.math.BigDecimal;

public interface ShippingStrategy {
    BigDecimal calcularFrete(BigDecimal pesoTotal);
    Integer getPrazoEntregaDias();
}
