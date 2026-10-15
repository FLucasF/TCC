package com.loja.clube;

import java.math.BigDecimal;

public interface NivelClube {
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    boolean temFreteGratis();
    boolean ganhouBrinde(BigDecimal subtotalProdutos);
}
