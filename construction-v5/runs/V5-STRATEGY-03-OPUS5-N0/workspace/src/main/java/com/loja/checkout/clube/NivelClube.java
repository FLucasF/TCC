package com.loja.checkout.clube;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

/**
 * Um nivel do clube da loja. Para criar um nivel novo basta uma implementacao
 * anotada com @Component, com as vantagens dele.
 */
public interface NivelClube {

    /** Codigo que o site envia em nivelClube. */
    String codigo();

    /** Credito para a proxima compra, sobre o valor dos produtos. */
    default BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    /** Se o nivel isenta o cliente do frete. */
    default boolean temFreteGratis() {
        return false;
    }

    /** Se a loja manda um brinde junto com este pedido. */
    default boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
