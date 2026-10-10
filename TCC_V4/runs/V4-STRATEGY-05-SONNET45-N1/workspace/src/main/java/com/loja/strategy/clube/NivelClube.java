package com.loja.strategy.clube;

import java.math.BigDecimal;

public interface NivelClube {
    boolean isFretGratis();
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    boolean concedeBrinde(BigDecimal subtotalProdutos);
}
