package com.loja.checkout.entrega;

import com.loja.checkout.comum.Identificavel;
import com.loja.checkout.pedido.Pedido;
import java.math.BigDecimal;

/**
 * Uma forma de entrega da loja. Para entrar uma transportadora nova,
 * basta criar um bean novo implementando esta interface.
 */
public interface ModalidadeEntrega extends Identificavel {

    /** Prazo prometido, em dias. */
    int prazoEntregaDias();

    /** Frete cobrado por esta modalidade, arredondado para centavos. */
    BigDecimal calcularFrete(Pedido pedido);

    /** Se esta modalidade atende este pedido (peso, regiao, o que a parceira limitar). */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
