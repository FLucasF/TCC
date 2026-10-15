package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Moeda;
import java.math.BigDecimal;

/**
 * Nivel do cliente no clube da loja. Para criar um nivel novo basta uma
 * implementacao anotada com @Component com as vantagens dele.
 */
public interface NivelClube {

    String codigo();

    /** Credito guardado para a proxima compra, sobre o valor dos produtos. */
    default BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Moeda.ZERO;
    }

    /** Se o nivel isenta o cliente do frete. */
    default boolean temFreteGratis() {
        return false;
    }

    /** Se o pedido ganha brinde. */
    default boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
