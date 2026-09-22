package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Uma forma de pagamento: quantas parcelas aceita, que pedidos atende e quanto cobra no fim. */
public interface FormaPagamento {

    String codigo();

    boolean parcelamentoPermitido(int parcelas);

    ResultadoPagamento cobrar(BigDecimal totalPedido, int parcelas);

    default boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
