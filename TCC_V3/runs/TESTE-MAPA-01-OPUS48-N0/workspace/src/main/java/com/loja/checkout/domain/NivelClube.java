package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Nível do cliente no clube da loja.
 *
 * Cada nível define quanto volta em crédito (sobre o valor dos produtos),
 * se o cliente paga frete e a partir de qual valor de produtos ganha brinde.
 * Para criar um novo nível, basta adicionar uma constante aqui.
 */
public enum NivelClube {

    BRONZE(0.0, true, null),
    PRATA(0.02, true, null),
    OURO(0.05, false, BigDecimal.valueOf(500));

    private final BigDecimal percentualCredito;
    private final boolean pagaFrete;
    private final BigDecimal minimoProdutosBrinde;

    NivelClube(double percentualCredito, boolean pagaFrete, BigDecimal minimoProdutosBrinde) {
        this.percentualCredito = BigDecimal.valueOf(percentualCredito);
        this.pagaFrete = pagaFrete;
        this.minimoProdutosBrinde = minimoProdutosBrinde;
    }

    public static Optional<NivelClube> fromCodigo(String codigo) {
        return Enums.fromNome(NivelClube.class, codigo);
    }

    /** Crédito para a próxima compra, sobre o valor dos produtos. */
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(percentualCredito));
    }

    public boolean pagaFrete() {
        return pagaFrete;
    }

    /** Ganha brinde quando o nível prevê e os produtos passam do mínimo. */
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return minimoProdutosBrinde != null
                && subtotalProdutos.compareTo(minimoProdutosBrinde) > 0;
    }
}
