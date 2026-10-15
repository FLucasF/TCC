package com.loja.checkout.dominio.clube;

import java.math.BigDecimal;

public interface NivelClube {
    BigDecimal credito(BigDecimal subtotalProdutos);
    boolean isentaFrete();
    boolean ganhaBrinde(BigDecimal subtotalProdutos);
}
