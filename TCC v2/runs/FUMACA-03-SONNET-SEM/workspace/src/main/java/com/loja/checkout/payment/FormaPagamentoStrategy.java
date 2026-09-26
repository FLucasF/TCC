package com.loja.checkout.payment;

import java.math.BigDecimal;

/**
 * Implemente esta interface e anote com {@code @Component} para cadastrar uma nova forma de pagamento.
 */
public interface FormaPagamentoStrategy {

    String getCodigo();

    boolean parcelasValidas(int parcelas);

    boolean disponivelPara(BigDecimal totalPedido);

    ResultadoPagamento aplicar(BigDecimal totalPedido, int parcelas);
}
