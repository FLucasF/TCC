package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Para aceitar uma forma nova, basta criar uma
 * implementacao anotada com {@code @Component}: ela entra no catalogo sozinha.
 */
public interface FormaPagamento {

    /** Codigo enviado pelo site, ex.: PIX. */
    String codigo();

    /** Se o numero de parcelas e permitido nesta forma de pagamento. */
    boolean permiteParcelas(int parcelas);

    /** Calcula o valor final e a parcela a partir do total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);

    /** Se a forma de pagamento atende este pedido (limite de valor, etc.). */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
