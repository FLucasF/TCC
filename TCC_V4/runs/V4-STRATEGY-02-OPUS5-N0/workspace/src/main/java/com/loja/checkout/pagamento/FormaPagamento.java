package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Para aceitar uma forma nova, basta uma classe que
 * implemente esta interface marcada com {@code @Component}.
 */
public interface FormaPagamento {

    /** Código usado pelo site, ex.: {@code PIX}. */
    String codigo();

    /** Se a forma de pagamento aceita esse número de parcelas. */
    boolean aceitaParcelas(int parcelas);

    /** Valor final e valor da parcela a partir do total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    /** Se a forma de pagamento atende um pedido desse valor. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
