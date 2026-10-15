package com.loja.checkout.dominio.clube;

import java.math.BigDecimal;

public interface NivelClube {
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    boolean isentaFrete();
    boolean ganhaBrinde(BigDecimal subtotalProdutos);
}
