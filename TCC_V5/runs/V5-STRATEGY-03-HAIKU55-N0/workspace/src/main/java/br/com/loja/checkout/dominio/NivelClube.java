package br.com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum NivelClube {

    BRONZE(BigDecimal.ZERO, false, null),
    PRATA(new BigDecimal("0.02"), false, null),
    OURO(new BigDecimal("0.05"), true, new BigDecimal("500.00"));

    private final BigDecimal percentualCredito;
    private final boolean freteGratis;
    private final BigDecimal brindeAcimaDe;

    NivelClube(BigDecimal percentualCredito, boolean freteGratis, BigDecimal brindeAcimaDe) {
        this.percentualCredito = percentualCredito;
        this.freteGratis = freteGratis;
        this.brindeAcimaDe = brindeAcimaDe;
    }

    public BigDecimal percentualCredito() {
        return percentualCredito;
    }

    public boolean freteGratis() {
        return freteGratis;
    }

    public boolean temBrinde(BigDecimal subtotal) {
        return brindeAcimaDe != null && subtotal.compareTo(brindeAcimaDe) > 0;
    }
}
