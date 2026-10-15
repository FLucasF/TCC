package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

public interface NivelClube {
    boolean zeraFrete();
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    boolean ganhaBrinde(BigDecimal subtotalProdutos);
}
