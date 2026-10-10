package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Níveis do clube da loja.
 *
 * <p>Cada nível concentra suas próprias vantagens. Para criar um nível novo,
 * basta adicionar uma constante aqui com as vantagens dele — o restante do
 * cálculo não precisa mudar.
 */
public enum NivelClube {

    /** Só o cadastro, não ganha nada. */
    BRONZE(BigDecimal.ZERO, false, null),

    /** 2% dos produtos de volta em crédito. */
    PRATA(new BigDecimal("2"), false, null),

    /** 5% de crédito, nunca paga frete e ganha brinde acima de R$ 500,00 em produtos. */
    OURO(new BigDecimal("5"), true, new BigDecimal("500.00"));

    private final BigDecimal percentualCredito;
    private final boolean freteGratisSempre;
    private final BigDecimal minimoProdutosParaBrinde;

    NivelClube(BigDecimal percentualCredito, boolean freteGratisSempre, BigDecimal minimoProdutosParaBrinde) {
        this.percentualCredito = percentualCredito;
        this.freteGratisSempre = freteGratisSempre;
        this.minimoProdutosParaBrinde = minimoProdutosParaBrinde;
    }

    /** Crédito para a próxima compra, calculado sobre o valor dos produtos. */
    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.porcentagem(subtotalProdutos, percentualCredito);
    }

    public boolean isFreteGratisSempre() {
        return freteGratisSempre;
    }

    /** Diz se o pedido tem direito a brinde, dado o valor dos produtos. */
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return minimoProdutosParaBrinde != null
                && subtotalProdutos.compareTo(minimoProdutosParaBrinde) > 0;
    }
}
