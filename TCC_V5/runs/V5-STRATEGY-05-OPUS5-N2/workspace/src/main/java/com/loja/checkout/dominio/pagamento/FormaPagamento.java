package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Identificavel;

import java.math.BigDecimal;

/** Uma forma de pagamento, com o seu ajuste sobre o total do pedido. */
public interface FormaPagamento extends Identificavel {

    boolean aceitaParcelas(int parcelas);

    /** Se esta forma atende este pedido (ex.: boleto tem teto de valor). */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
