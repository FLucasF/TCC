package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Codificavel;
import java.math.BigDecimal;

/**
 * Um nivel do clube da loja e as vantagens que ele da.
 *
 * <p>Nivel novo e uma classe anotada com {@code @Component} implementando esta interface.
 */
public interface NivelClube extends Codificavel {

    /** Credito guardado para a proxima compra, em centavos (nao abate nada nesta compra). */
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    /** {@code true} quando o nivel nunca paga frete. */
    default boolean freteGratis() {
        return false;
    }

    /** {@code true} quando a loja manda um brinde junto com o pedido. */
    default boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
