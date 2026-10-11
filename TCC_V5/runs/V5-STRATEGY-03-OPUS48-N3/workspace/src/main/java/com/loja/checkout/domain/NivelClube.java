package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Níveis do clube. Hoje as vantagens variam só em valores e flags: quanto de
 * crédito o cliente ganha, se é isento de frete e a partir de que valor de
 * produtos ganha brinde (nulo = nunca). Por isso são dados, não comportamento.
 */
public enum NivelClube {

    BRONZE("0", false, null),
    PRATA("0.02", false, null),
    OURO("0.05", true, "500.00");

    private final BigDecimal creditoPercentual;
    private final boolean isentaFrete;
    private final BigDecimal limiteBrinde;

    NivelClube(String creditoPercentual, boolean isentaFrete, String limiteBrinde) {
        this.creditoPercentual = new BigDecimal(creditoPercentual);
        this.isentaFrete = isentaFrete;
        this.limiteBrinde = limiteBrinde == null ? null : new BigDecimal(limiteBrinde);
    }

    /** Crédito para a próxima compra sobre o valor dos produtos (arredondado). */
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Money.centavos(subtotalProdutos.multiply(creditoPercentual));
    }

    /** Frete que o cliente paga: zero quando o nível é isento. */
    public BigDecimal aplicarIsencao(BigDecimal frete) {
        return isentaFrete ? BigDecimal.ZERO : frete;
    }

    /** Se o pedido ganha brinde, conforme o valor dos produtos. */
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return limiteBrinde != null && subtotalProdutos.compareTo(limiteBrinde) > 0;
    }
}
