package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

public interface NivelClube {

    BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    boolean freteGratis();

    boolean brinde(BigDecimal subtotalProdutos);
}
