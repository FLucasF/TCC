package com.loja.domain.clube;

import java.math.BigDecimal;

public interface NivelClube {
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    boolean isFreteCortesia();
    boolean temBrinde(BigDecimal subtotalProdutos);
}
