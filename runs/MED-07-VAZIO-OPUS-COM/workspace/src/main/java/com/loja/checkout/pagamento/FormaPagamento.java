package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Cobranca;
import com.loja.checkout.dominio.Codificado;
import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Cada forma diz em quantas parcelas aceita ser paga,
 * se atende o total do pedido e quanto o cliente paga no fim.
 */
public interface FormaPagamento extends Codificado {

    boolean aceitaParcelas(int parcelas);

    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
