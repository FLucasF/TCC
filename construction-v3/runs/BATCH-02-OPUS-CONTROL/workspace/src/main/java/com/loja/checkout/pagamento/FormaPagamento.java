package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Forma nova = uma classe nova com @Component
 * implementando esta interface.
 */
public interface FormaPagamento {

    /** Codigo usado pelo site, ex.: "PIX". */
    String codigo();

    /** Se a forma aceita esse numero de parcelas. */
    boolean aceitaParcelas(int parcelas);

    /** Se a forma atende o pedido (limite de valor, etc.). */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    /** Valores finais, ja arredondados para centavos. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
