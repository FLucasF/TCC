package com.loja.checkout.dominio.clube;

import java.math.BigDecimal;

public interface Nivel {
    String codigo();
    BigDecimal credito(BigDecimal subtotalProdutos);
    BigDecimal ajustarFrete(BigDecimal fretePadrao);
    boolean brinde(BigDecimal subtotalProdutos);
}
