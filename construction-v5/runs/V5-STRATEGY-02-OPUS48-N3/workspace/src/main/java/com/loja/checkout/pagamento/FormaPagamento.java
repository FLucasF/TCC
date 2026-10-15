package com.loja.checkout.pagamento;

import com.loja.checkout.comum.Codificavel;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Cada uma tem suas parcelas permitidas, sua regra de
 * disponibilidade e seu ajuste sobre o total (desconto, juros ou tarifa).
 */
public interface FormaPagamento extends Codificavel {

    /** Se este número de parcelas é permitido (Pix/boleto só 1; cartão 1 a 12). */
    boolean parcelasPermitidas(int parcelas);

    /** Se a forma atende o pedido com este total (ex.: boleto até R$ 1.000). */
    boolean atende(BigDecimal totalPedido);

    /** Valor final e valor da parcela a partir do total e do número de parcelas. */
    ResultadoPagamento calcula(BigDecimal totalPedido, int parcelas);
}
