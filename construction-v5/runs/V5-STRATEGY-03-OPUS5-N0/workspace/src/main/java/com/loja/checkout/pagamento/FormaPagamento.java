package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Cada forma nova e uma implementacao anotada com
 * @Component, que entra sozinha no catalogo.
 */
public interface FormaPagamento {

    /** Codigo que o site envia em formaPagamento. */
    String codigo();

    /** Se o numero de parcelas pedido e permitido nesta forma de pagamento. */
    default boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    /** Se esta forma de pagamento atende o pedido (valor minimo, maximo...). */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    /** Valor final e parcelas, a partir do total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
