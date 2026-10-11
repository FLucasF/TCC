package com.loja.checkout.dominio.clube;

import java.math.BigDecimal;

/**
 * Um nível do clube. Cada nível novo entra como uma implementação com o seu
 * conjunto de vantagens.
 */
public interface NivelClube {

    String codigo();

    /** Crédito para a próxima compra, sobre o valor dos produtos. */
    BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    /** O frete que este nível cobra, dado o frete da modalidade escolhida. */
    BigDecimal freteCobrado(BigDecimal freteDaModalidade);

    boolean temBrinde(BigDecimal subtotalProdutos);
}
