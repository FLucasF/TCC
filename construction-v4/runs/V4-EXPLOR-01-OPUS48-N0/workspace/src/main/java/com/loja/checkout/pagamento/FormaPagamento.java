package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento.
 *
 * <p>Cada forma é uma implementação desta interface registrada como
 * {@code @Component} e descoberta pelo {@link FormaPagamentoRegistry}.
 */
public interface FormaPagamento {

    /** Código usado no campo {@code formaPagamento} do pedido (ex.: "PIX"). */
    String codigo();

    /** Diz se o número de parcelas é permitido (Pix e boleto só 1; cartão de 1 a 12). */
    boolean parcelasPermitidas(int parcelas);

    /** Diz se esta forma atende o pedido com o total informado (ex.: boleto só até R$ 1.000,00). */
    boolean atende(BigDecimal totalPedido);

    /** Calcula o ajuste, o valor final e o valor da parcela a partir do total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
