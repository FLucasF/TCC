package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

/**
 * Um nivel do clube da loja. Para criar um nivel novo, basta uma implementacao
 * desta interface anotada com @Component, com o conjunto de vantagens dele.
 */
public interface NivelClube {

    /** Codigo enviado pelo site (ex.: OURO). */
    String codigo();

    /** Credito para a proxima compra, sobre o valor dos produtos. */
    default BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    /** Diz se o nivel isenta o cliente do frete. */
    default boolean freteGratis() {
        return false;
    }

    /** Diz se o pedido leva brinde. */
    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
