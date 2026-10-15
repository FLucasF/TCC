package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento aceita pela loja. Forma nova e uma classe nova
 * implementando esta interface, marcada com @Component.
 */
public interface FormaPagamento {

    /** Codigo usado pelo site (ex.: PIX). */
    String codigo();

    /** Se a forma de pagamento aceita esse numero de parcelas. */
    default boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    /** Se a forma de pagamento atende um pedido desse valor. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    /** Valor final e valor da parcela para o total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
