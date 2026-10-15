package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Identificavel;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Cada forma diz em quantas vezes aceita ser paga,
 * quais pedidos atende e o que faz com o total (desconto, tarifa ou juros).
 */
public interface FormaPagamento extends Identificavel {

    /** Se aceita esse número de parcelas. */
    boolean permiteParcelas(int parcelas);

    /** Se atende o pedido com esse total (ex.: boleto só até R$ 1.000,00). */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    /** Aplica o ajuste da forma de pagamento sobre o total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
