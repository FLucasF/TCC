package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(BigDecimal.ZERO, false, false),
    PRATA(new BigDecimal("0.02"), false, false),
    OURO(new BigDecimal("0.05"), true, true);

    private final BigDecimal porcentagemCredito;
    private final boolean freteGratis;
    private final boolean ganhaBrinde;

    NivelClube(BigDecimal porcentagemCredito, boolean freteGratis, boolean ganhaBrinde) {
        this.porcentagemCredito = porcentagemCredito;
        this.freteGratis = freteGratis;
        this.ganhaBrinde = ganhaBrinde;
    }

    public BigDecimal getPorcentagemCredito() {
        return porcentagemCredito;
    }

    public boolean isFreteGratis() {
        return freteGratis;
    }

    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return ganhaBrinde && subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
    }
}
