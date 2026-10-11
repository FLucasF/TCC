package com.loja.clube;

import java.math.BigDecimal;

public interface NivelClube {
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    boolean ganhaFreteGratis();
    boolean ganhaBrinde(BigDecimal subtotalProdutos);
}
