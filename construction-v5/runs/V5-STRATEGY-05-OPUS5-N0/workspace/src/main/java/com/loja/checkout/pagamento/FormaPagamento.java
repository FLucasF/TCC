package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Para aceitar uma forma nova basta implementar esta
 * interface e marcar a classe com @Component.
 */
public interface FormaPagamento {

    /** Codigo usado na requisicao, ex.: PIX. */
    String codigo();

    /** Se a forma aceita esse numero de parcelas. */
    default boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    /** Se a forma atende este pedido (ex.: limite de valor do boleto). */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    /** Valor final, parcela e ajuste a partir do total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
