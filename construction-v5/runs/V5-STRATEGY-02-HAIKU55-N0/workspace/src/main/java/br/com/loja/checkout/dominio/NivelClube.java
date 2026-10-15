package br.com.loja.checkout.dominio;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE(new BigDecimal("0.00"), false, null),
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

    public boolean temFreteGratis() {
        return freteGratis;
    }

    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return brindeAcimaDe != null && subtotalProdutos.compareTo(brindeAcimaDe) > 0;
    }
}
