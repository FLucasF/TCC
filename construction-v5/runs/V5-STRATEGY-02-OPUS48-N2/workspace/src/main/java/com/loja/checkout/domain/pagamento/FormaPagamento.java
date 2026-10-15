package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

/**
 * Como o cliente paga. Cada forma tem seu ajuste sobre o total (desconto, tarifa
 * ou juros), suas regras de parcelamento e suas limitacoes.
 */
public interface FormaPagamento {

    /** O codigo enviado pelo site (ex.: PIX). */
    String codigo();

    /**
     * Confere se o numero de parcelas e permitido para esta forma.
     * Lanca PARCELAMENTO_INVALIDO quando nao e.
     */
    void validarParcelas(int parcelas);

    /**
     * Confere se esta forma atende um pedido com este total.
     * Lanca FORMA_PAGAMENTO_INDISPONIVEL quando nao atende.
     */
    void validarDisponibilidade(BigDecimal totalPedido);

    /** Valor final e valor da parcela a partir do total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
