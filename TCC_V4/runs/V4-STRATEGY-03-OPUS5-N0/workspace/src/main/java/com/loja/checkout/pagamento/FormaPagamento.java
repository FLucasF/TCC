package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Para aceitar uma forma nova, basta criar um bean
 * que implemente esta interface.
 */
public interface FormaPagamento {

    /** Codigo enviado pelo site (ex.: PIX). */
    String codigo();

    /** Aplica o ajuste da forma de pagamento sobre o total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    /** Se false, o numero de parcelas nao e permitido (PARCELAMENTO_INVALIDO). */
    default boolean permiteParcelas(int parcelas) {
        return parcelas == 1;
    }

    /** Se false, a forma existe mas nao atende este pedido (FORMA_PAGAMENTO_INDISPONIVEL). */
    default boolean disponivel(BigDecimal totalPedido) {
        return true;
    }
}
