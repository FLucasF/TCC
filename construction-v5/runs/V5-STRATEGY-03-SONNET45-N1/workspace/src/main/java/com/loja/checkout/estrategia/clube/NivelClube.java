package com.loja.checkout.estrategia.clube;

import java.math.BigDecimal;

public interface NivelClube {
    BigDecimal calcularCredito(BigDecimal valorProdutos);
    boolean freteGratis();
    boolean ganhaBrinde(BigDecimal valorProdutos);
}
