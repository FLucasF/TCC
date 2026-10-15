package com.loja.checkout.clube;

import java.math.BigDecimal;

/**
 * Nivel do cliente no clube da loja. Para criar um nivel novo basta uma
 * implementacao anotada com {@code @Component}.
 */
public interface NivelClube {

    /** Codigo enviado pelo site, em maiusculas (ex.: OURO). */
    String codigo();

    /** Credito para a proxima compra, calculado sobre os produtos sem desconto. */
    default BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return com.loja.checkout.dominio.Dinheiro.ZERO;
    }

    /** Se o nivel isenta o cliente do frete. */
    default boolean freteGratis() {
        return false;
    }

    /** Se o pedido ganha brinde. */
    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
