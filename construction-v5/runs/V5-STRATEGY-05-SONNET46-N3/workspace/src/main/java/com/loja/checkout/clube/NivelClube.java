package com.loja.checkout.clube;

import java.math.BigDecimal;

public interface NivelClube {
    boolean isFreteGratis();
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    boolean ganharBrinde(BigDecimal subtotalProdutos);
}
