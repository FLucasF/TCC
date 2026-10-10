package com.loja.checkout.clube;

import java.math.BigDecimal;

/**
 * Um nível do clube da loja. Para criar um nível novo, basta uma classe que
 * implemente esta interface marcada com {@code @Component}.
 */
public interface NivelClube {

    /** Código usado pelo site, ex.: {@code OURO}. */
    String codigo();

    /** Crédito para a próxima compra, sobre o valor dos produtos, arredondado em centavos. */
    BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos);

    /** Se o nível isenta o frete. */
    default boolean freteGratis() {
        return false;
    }

    /** Se o pedido vai com brinde. */
    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
