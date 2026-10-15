package com.loja.checkout;

import java.math.BigDecimal;
import java.util.function.Predicate;

enum NivelClube {

    BRONZE(new BigDecimal("0"), false, subtotal -> false),
    PRATA(new BigDecimal("0.02"), false, subtotal -> false),
    OURO(new BigDecimal("0.05"), true, subtotal -> subtotal.compareTo(new BigDecimal("500")) > 0);

    private final BigDecimal percentualCredito;
    private final boolean freteGratis;
    private final Predicate<BigDecimal> temBrinde;

    NivelClube(BigDecimal percentualCredito, boolean freteGratis, Predicate<BigDecimal> temBrinde) {
        this.percentualCredito = percentualCredito;
        this.freteGratis = freteGratis;
        this.temBrinde = temBrinde;
    }

    BigDecimal credito(BigDecimal subtotal) {
        return Dinheiro.centavos(subtotal.multiply(percentualCredito));
    }

    boolean temFreteGratis() {
        return freteGratis;
    }

    boolean temBrinde(BigDecimal subtotal) {
        return temBrinde.test(subtotal);
    }
}
