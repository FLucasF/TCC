package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Nível do cliente no clube. Cada nível traz seu próprio conjunto de vantagens
 * (crédito de volta, frete grátis, brinde acima de um valor). Para um nível novo,
 * basta uma entrada nova aqui com as suas vantagens.
 */
public enum NivelClube {

    BRONZE("0", false, null),
    PRATA("0.02", false, null),
    OURO("0.05", true, "500.00");

    private final BigDecimal taxaCredito;
    private final boolean freteGratis;
    private final BigDecimal valorMinimoBrinde;

    NivelClube(String taxaCredito, boolean freteGratis, String valorMinimoBrinde) {
        this.taxaCredito = new BigDecimal(taxaCredito);
        this.freteGratis = freteGratis;
        this.valorMinimoBrinde = valorMinimoBrinde == null ? null : new BigDecimal(valorMinimoBrinde);
    }

    /** Crédito para a próxima compra: porcentagem sobre o valor dos produtos. */
    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.centavos(subtotalProdutos.multiply(taxaCredito));
    }

    /** Quando o nível não paga frete, o frete sai zerado no resumo. */
    public boolean pagaFrete() {
        return !freteGratis;
    }

    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return valorMinimoBrinde != null && subtotalProdutos.compareTo(valorMinimoBrinde) > 0;
    }
}
