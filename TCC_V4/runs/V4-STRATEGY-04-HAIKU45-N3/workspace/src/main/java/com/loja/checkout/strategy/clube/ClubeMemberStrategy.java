package com.loja.checkout.strategy.clube;

import java.math.BigDecimal;

public interface ClubeMemberStrategy {
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    boolean temFreteGratis();

    boolean validarBrinde(BigDecimal subtotalProdutos);
}
