package com.loja.checkout.club;

import java.math.BigDecimal;

public interface NivelClube {

    String codigo();

    BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    boolean isentoDeFrete();

    boolean temBrinde(BigDecimal subtotalProdutos);
}
