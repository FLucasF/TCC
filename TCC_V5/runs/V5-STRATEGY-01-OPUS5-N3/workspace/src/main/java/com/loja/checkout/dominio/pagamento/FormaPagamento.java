package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Cada uma tem seu ajuste sobre o total do pedido, suas
 * parcelas permitidas e suas restrições.
 */
public interface FormaPagamento {

    String codigo();

    boolean aceitaParcelas(int parcelas);

    /** Se a forma atende este total (ex.: limite do boleto). */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
