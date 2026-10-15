package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

public interface BeneficiosClube {
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    boolean isentoFrete();
    boolean ganhabrinde(BigDecimal subtotalProdutos);
}
