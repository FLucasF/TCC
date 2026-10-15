package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Uma forma de pagamento: quantas parcelas aceita, o que atende e como cobra. */
public interface FormaPagamento {

    String codigo();

    boolean aceitaParcelas(int parcelas);

    /** Se a forma atende este total (ex.: boleto só até R$ 1.000,00). */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
