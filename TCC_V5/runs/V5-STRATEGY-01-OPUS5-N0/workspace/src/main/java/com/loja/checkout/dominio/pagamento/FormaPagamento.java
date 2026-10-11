package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Para aceitar uma forma nova, basta uma implementacao
 * desta interface anotada com @Component.
 */
public interface FormaPagamento {

    /** Codigo enviado pelo site (ex.: PIX). */
    String codigo();

    /** Diz se o numero de parcelas e permitido nesta forma de pagamento. */
    default boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    /** Diz se esta forma de pagamento atende um pedido com o total informado. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    /** Calcula o valor final e o valor da parcela a partir do total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
