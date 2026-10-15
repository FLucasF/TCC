package com.loja.checkout.domain;

import static com.loja.checkout.domain.Dinheiro.centavos;

import java.math.BigDecimal;

/**
 * Níveis do clube. As vantagens de cada nível (quanto volta em crédito, se
 * paga frete, a partir de quanto manda brinde) são dados do nível. A conta de
 * cada vantagem é a mesma para todos; só os dados mudam. Nível novo? Basta
 * acrescentar uma constante com o seu conjunto de vantagens.
 */
public enum NivelClube {

    BRONZE("0", true, null),
    PRATA("0.02", true, null),
    OURO("0.05", false, "500");

    private final BigDecimal taxaCredito;
    private final boolean pagaFrete;
    private final BigDecimal limiteBrinde;

    NivelClube(String taxaCredito, boolean pagaFrete, String limiteBrinde) {
        this.taxaCredito = new BigDecimal(taxaCredito);
        this.pagaFrete = pagaFrete;
        this.limiteBrinde = limiteBrinde == null ? null : new BigDecimal(limiteBrinde);
    }

    /** Crédito para a próxima compra, sobre o valor dos produtos. */
    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return centavos(subtotalProdutos.multiply(taxaCredito));
    }

    /** O frete que o cliente de fato paga: zera quando o nível não paga frete. */
    public BigDecimal freteDevido(BigDecimal freteBase) {
        return pagaFrete ? freteBase : centavos(BigDecimal.ZERO);
    }

    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return limiteBrinde != null && subtotalProdutos.compareTo(limiteBrinde) > 0;
    }
}
