package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

public interface NivelClube {

    String getCodigo();

    BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    boolean isFreteGratis();

    boolean temBrinde(BigDecimal subtotalProdutos);
}
