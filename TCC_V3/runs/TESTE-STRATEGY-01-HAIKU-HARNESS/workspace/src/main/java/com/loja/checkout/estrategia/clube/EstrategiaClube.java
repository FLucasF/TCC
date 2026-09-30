package com.loja.checkout.estrategia.clube;

import java.math.BigDecimal;

public interface EstrategiaClube {
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    BigDecimal getDescontoFrete();
    boolean verificarBrinde(BigDecimal subtotalProdutos);
}
