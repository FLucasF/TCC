package com.loja.checkout.clube;

import java.math.BigDecimal;

public interface BeneficioClube {

    BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    boolean freteGratis();

    boolean temBrinde(BigDecimal subtotalProdutos);
}
