package com.loja.checkout.strategy;

import java.math.BigDecimal;

public interface NivelClube {
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    boolean temFreteGratis();
    boolean temBrinde(BigDecimal subtotalProdutos);
    String nome();
}
