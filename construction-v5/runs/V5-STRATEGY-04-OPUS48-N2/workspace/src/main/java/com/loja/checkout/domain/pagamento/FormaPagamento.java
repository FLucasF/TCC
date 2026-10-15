package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Cada forma diz quantas parcelas aceita, se atende o
 * pedido e como transforma o total do pedido no valor final e nas parcelas.
 */
public interface FormaPagamento {

    /** Código exatamente como o site envia (ex.: "PIX"). */
    String codigo();

    /** Se este número de parcelas é permitido pela forma. */
    boolean parcelasPermitidas(int parcelas);

    /** Se a forma atende um pedido com este total. */
    boolean disponivel(BigDecimal totalPedido);

    /** Valor final e parcela a partir do total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
