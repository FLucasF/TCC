package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Níveis do clube. Cada nível define quanto rende de crédito, se paga frete e
 * a partir de quanto manda brinde. Um nível novo é uma constante nova aqui.
 */
public enum NivelClube {

    BRONZE("0.00", false, null),
    PRATA("0.02", false, null),
    OURO("0.05", true, new BigDecimal("500.00"));

    private final BigDecimal percentualCredito;
    private final boolean freteGratis;
    private final BigDecimal brindeAcimaDe;

    NivelClube(String percentualCredito, boolean freteGratis, BigDecimal brindeAcimaDe) {
        this.percentualCredito = new BigDecimal(percentualCredito);
        this.freteGratis = freteGratis;
        this.brindeAcimaDe = brindeAcimaDe;
    }

    /** Crédito para a próxima compra, sobre o valor dos produtos. */
    public BigDecimal credito(BigDecimal subtotal) {
        return Dinheiro.centavos(subtotal.multiply(percentualCredito));
    }

    public boolean freteGratis() {
        return freteGratis;
    }

    public boolean temBrinde(BigDecimal subtotal) {
        return brindeAcimaDe != null && subtotal.compareTo(brindeAcimaDe) > 0;
    }
}
