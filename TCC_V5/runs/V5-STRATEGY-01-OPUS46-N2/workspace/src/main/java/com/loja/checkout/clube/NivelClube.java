package com.loja.checkout.clube;

import java.math.BigDecimal;

public interface NivelClube {

    String getCodigo();

    boolean isFreteGratis();

    BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    boolean temBrinde(BigDecimal subtotalProdutos);
}
