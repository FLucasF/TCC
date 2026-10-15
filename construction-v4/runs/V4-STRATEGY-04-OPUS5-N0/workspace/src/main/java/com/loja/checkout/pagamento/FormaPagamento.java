package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento aceita pela loja. Para aceitar uma forma nova, basta uma
 * classe nova que implemente esta interface e marcar com @Component: ela entra
 * sozinha no {@link FormasPagamento}.
 */
public interface FormaPagamento {

    /** Codigo que o site manda no campo formaPagamento. */
    String codigo();

    /** Se o numero de parcelas pedido e permitido nesta forma de pagamento. */
    default boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    /** Se esta forma de pagamento atende o pedido (valor maximo, por exemplo). */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    /** Valor final e valor da parcela a partir do total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
