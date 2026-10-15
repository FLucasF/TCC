package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento aceita pela loja. Forma nova = uma classe nova anotada
 * com {@code @Component}.
 */
public interface FormaPagamento {

    /** Codigo usado pelo site, ex.: "PIX". */
    String codigo();

    /** Diz se o numero de parcelas pedido e permitido nesta forma. */
    boolean aceitaParcelas(int parcelas);

    /** Diz se a forma atende este pedido (ex.: boleto tem teto de valor). */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    /** Aplica o ajuste (desconto, tarifa ou juros) sobre o total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
