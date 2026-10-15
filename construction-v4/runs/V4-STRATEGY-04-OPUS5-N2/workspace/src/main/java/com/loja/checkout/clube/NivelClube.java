package com.loja.checkout.clube;

import com.loja.checkout.catalogo.Identificado;
import java.math.BigDecimal;

/**
 * Nivel do cliente no clube da loja. Cada nivel tem seu conjunto de
 * vantagens; nivel novo e uma classe nova.
 */
public interface NivelClube extends Identificado {

    /** Credito que o cliente ganha para a proxima compra. */
    BigDecimal credito(BigDecimal subtotalProdutos);

    /** Quanto o cliente deste nivel paga de frete, dado o valor da modalidade. */
    default BigDecimal frete(BigDecimal freteDaModalidade) {
        return freteDaModalidade;
    }

    /** Se o pedido vai com brinde. */
    default boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
