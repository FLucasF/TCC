package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(BigDecimal.ZERO, false, false),
    PRATA(new BigDecimal("0.02"), false, false),
    OURO(new BigDecimal("0.05"), true, true);

    private final BigDecimal porcentagemCredito;
    private final boolean freteGratis;
    private final boolean elegibilidadeBrinde;

    NivelClube(BigDecimal porcentagemCredito, boolean freteGratis, boolean elegibilidadeBrinde) {
        this.porcentagemCredito = porcentagemCredito;
        this.freteGratis = freteGratis;
        this.elegibilidadeBrinde = elegibilidadeBrinde;
    }

    public BigDecimal getPorcentagemCredito() {
        return porcentagemCredito;
    }

    public boolean isFreteGratis() {
        return freteGratis;
    }

    public boolean isElegibilidadeBrinde() {
        return elegibilidadeBrinde;
    }
}
