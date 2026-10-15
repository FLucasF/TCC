package com.loja.checkout.clube;

import java.math.BigDecimal;

/**
 * Um nivel do clube da loja e as vantagens dele. Para criar um nivel novo
 * basta implementar esta interface e marcar a classe com @Component.
 */
public interface NivelClube {

    /** Codigo usado na requisicao, ex.: OURO. */
    String codigo();

    /** Percentual dos produtos devolvido em credito para a proxima compra. */
    default BigDecimal percentualCredito() {
        return BigDecimal.ZERO;
    }

    /** Se o nivel nao paga frete. */
    default boolean freteGratis() {
        return false;
    }

    /** Se o pedido vai com brinde. */
    default boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
