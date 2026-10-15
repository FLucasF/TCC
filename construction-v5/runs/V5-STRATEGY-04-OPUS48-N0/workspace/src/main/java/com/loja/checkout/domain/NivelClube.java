package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Níveis do clube da loja. Cada nível define quanto o cliente ganha de crédito
 * para a próxima compra, se paga frete e quando recebe brinde.
 *
 * <p>Para criar um nível novo, basta adicionar mais uma constante aqui com suas
 * vantagens — o resto do cálculo já usa estes valores.
 */
public enum NivelClube {

    /** Só o cadastro, não ganha nada. */
    BRONZE(BigDecimal.ZERO, false, null),

    /** 2% dos produtos de volta em crédito. */
    PRATA(new BigDecimal("2"), false, null),

    /** 5% de crédito, não paga frete nunca, e brinde acima de R$ 500 em produtos. */
    OURO(new BigDecimal("5"), true, new BigDecimal("500.00"));

    private final BigDecimal creditoPercent;
    private final boolean freteGratis;
    private final BigDecimal brindeAcimaDe;

    NivelClube(BigDecimal creditoPercent, boolean freteGratis, BigDecimal brindeAcimaDe) {
        this.creditoPercent = creditoPercent;
        this.freteGratis = freteGratis;
        this.brindeAcimaDe = brindeAcimaDe;
    }

    /** Crédito para a próxima compra: percentual sobre o valor dos produtos, em centavos. */
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Money.percent(subtotalProdutos, creditoPercent);
    }

    public boolean naoPagaFrete() {
        return freteGratis;
    }

    /** Indica se o pedido ganha brinde dado o valor dos produtos. */
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return brindeAcimaDe != null && subtotalProdutos.compareTo(brindeAcimaDe) > 0;
    }
}
