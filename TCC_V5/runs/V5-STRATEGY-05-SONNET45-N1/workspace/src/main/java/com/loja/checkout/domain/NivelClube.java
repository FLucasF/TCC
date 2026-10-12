package com.loja.checkout.domain;

import java.math.BigDecimal;

public interface NivelClube {
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    boolean freteSempre​Gratis();
    boolean ganhaBrinde(BigDecimal subtotalProdutos);
}
