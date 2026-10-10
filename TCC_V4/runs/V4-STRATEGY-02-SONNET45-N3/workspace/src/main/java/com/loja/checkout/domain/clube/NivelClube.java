package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

public interface NivelClube {
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    boolean temFreteGratis();
    boolean temBrinde(BigDecimal subtotalProdutos);
}
