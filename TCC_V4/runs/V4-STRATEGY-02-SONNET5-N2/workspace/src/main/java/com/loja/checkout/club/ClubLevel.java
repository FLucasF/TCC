package com.loja.checkout.club;

import java.math.BigDecimal;

public interface ClubLevel {

    String codigo();

    BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    boolean freteGratis();

    boolean concedeBrinde(BigDecimal subtotalProdutos);
}
