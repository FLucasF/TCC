package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** O que cada forma de pagamento precisa saber responder. */
public interface RegraPagamento {

    /** O valor final e a parcela, a partir do total do pedido. */
    ResultadoPagamento resolver(BigDecimal totalPedido, int parcelas);

    boolean aceitaParcelas(int parcelas);

    default boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
