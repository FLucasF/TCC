package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Niveis do clube da loja.
 *
 * <p>Cada nivel guarda suas vantagens: quanto volta de credito (sobre os produtos),
 * se paga frete e a partir de qual valor de produtos ganha brinde
 * ({@code null} quando o nivel nunca da brinde). Para criar um nivel novo,
 * basta acrescentar uma linha aqui.</p>
 */
public enum NivelClube {

    //            credito   paga frete?   produtos minimos para brinde
    BRONZE(new BigDecimal("0.00"), true,  null),
    PRATA( new BigDecimal("0.02"), true,  null),
    OURO(  new BigDecimal("0.05"), false, new BigDecimal("500.00"));

    private final BigDecimal percentualCredito;
    private final boolean pagaFrete;
    private final BigDecimal minimoProdutosParaBrinde;

    NivelClube(BigDecimal percentualCredito, boolean pagaFrete, BigDecimal minimoProdutosParaBrinde) {
        this.percentualCredito = percentualCredito;
        this.pagaFrete = pagaFrete;
        this.minimoProdutosParaBrinde = minimoProdutosParaBrinde;
    }

    public BigDecimal percentualCredito() {
        return percentualCredito;
    }

    public boolean pagaFrete() {
        return pagaFrete;
    }

    /** Diz se esse nivel ganha brinde para o valor de produtos informado. */
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return minimoProdutosParaBrinde != null
                && subtotalProdutos.compareTo(minimoProdutosParaBrinde) > 0;
    }
}
