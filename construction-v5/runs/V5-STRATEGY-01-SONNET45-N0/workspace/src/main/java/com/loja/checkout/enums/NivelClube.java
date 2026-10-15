package com.loja.checkout.enums;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(BigDecimal.ZERO, false, BigDecimal.ZERO),
    PRATA(new BigDecimal("0.02"), false, BigDecimal.ZERO),
    OURO(new BigDecimal("0.05"), true, new BigDecimal("500.00"));

    private final BigDecimal percentualCredito;
    private final boolean freteGratis;
    private final BigDecimal valorMinimoBrinde;

    NivelClube(BigDecimal percentualCredito, boolean freteGratis, BigDecimal valorMinimoBrinde) {
        this.percentualCredito = percentualCredito;
        this.freteGratis = freteGratis;
        this.valorMinimoBrinde = valorMinimoBrinde;
    }

    public BigDecimal getPercentualCredito() {
        return percentualCredito;
    }

    public boolean isFreteGratis() {
        return freteGratis;
    }

    public boolean temBrinde(BigDecimal valorProdutos) {
        if (valorMinimoBrinde.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        return valorProdutos.compareTo(valorMinimoBrinde) > 0;
    }
}
