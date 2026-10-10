package com.loja.checkout.clube;

import java.math.BigDecimal;

public interface ClubeStrategy {
    boolean temFreteGratis();
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    boolean temBrinde(BigDecimal subtotalProdutos);
}
