package com.example.checkout.club;

import java.math.BigDecimal;

public enum ClubLevel {
    BRONZE("0", false, null),
    PRATA("0.02", false, null),
    OURO("0.05", true, "500");

    private final BigDecimal percentualCredito;
    private final boolean freteGratis;
    private final BigDecimal limiteBrinde;

    ClubLevel(String percentualCredito, boolean freteGratis, String limiteBrinde) {
        this.percentualCredito = new BigDecimal(percentualCredito);
        this.freteGratis = freteGratis;
        this.limiteBrinde = limiteBrinde == null ? null : new BigDecimal(limiteBrinde);
    }

    public BigDecimal percentualCredito() { return percentualCredito; }
    public boolean freteGratis() { return freteGratis; }
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return limiteBrinde != null && subtotalProdutos.compareTo(limiteBrinde) > 0;
    }
}
