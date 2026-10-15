package com.loja.checkout.strategy;

import java.math.BigDecimal;

public interface NivelClube {
    String getCodigo();
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    BigDecimal ajustarFrete(BigDecimal frete);
    boolean ganhaBrinde(BigDecimal subtotalProdutos);
}
