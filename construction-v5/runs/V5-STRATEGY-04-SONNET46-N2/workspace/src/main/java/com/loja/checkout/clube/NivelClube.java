package com.loja.checkout.clube;

import java.math.BigDecimal;

public interface NivelClube {
    String getCodigo();
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    BigDecimal ajustarFrete(BigDecimal frete);
    boolean temBrinde(BigDecimal subtotalProdutos);
}
