package com.loja.checkout.clube;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Identificado;
import java.math.BigDecimal;

/**
 * Nível do cliente no clube. Cada nível tem seu conjunto de vantagens; nível
 * novo é uma implementação nova.
 */
public interface NivelClube extends Identificado {

    /** Crédito para a próxima compra, sobre o valor dos produtos. */
    default BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.ZERO;
    }

    /** O frete que o cliente desse nível paga, dado o frete da modalidade. */
    default BigDecimal frete(BigDecimal freteDaModalidade) {
        return freteDaModalidade;
    }

    /** Se o pedido vai com brinde. */
    default boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
