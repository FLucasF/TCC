package com.loja.checkout.clube;

import java.math.BigDecimal;

public interface BeneficiosClube {

    String codigo();

    BigDecimal percentualCredito();

    boolean freteGratis();

    boolean brinde(BigDecimal subtotalProdutos);
}
