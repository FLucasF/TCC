package com.loja.model.clube;

import java.math.BigDecimal;

public interface NivelClube {
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    boolean temFreteGratis();
    boolean ganhaBrinde(BigDecimal subtotalProdutos);
}
