package com.loja.checkout.dominio.clube;

import java.math.BigDecimal;

public interface NivelClube {

    String codigo();

    boolean isentaFrete();

    BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    boolean temBrinde(BigDecimal subtotalProdutos);
}
