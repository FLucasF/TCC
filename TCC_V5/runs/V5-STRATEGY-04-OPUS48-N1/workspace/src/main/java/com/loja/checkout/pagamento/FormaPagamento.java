package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. O comportamento muda conforme o caso (Pix desconta,
 * boleto tem tarifa, cartão parcela com ou sem juros), então cada forma é uma
 * classe própria. A assinatura atende o caso mais exigente, o cartão parcelado:
 * precisa do total e do número de parcelas para devolver valor final e parcela.
 */
public interface FormaPagamento {

    String codigo();

    /** Quantidades de parcela permitidas (Pix e boleto só 1; cartão de 1 a 12). */
    boolean parcelasValidas(int parcelas);

    /** Se a forma atende o pedido com este total (ex.: boleto só até R$ 1.000). */
    default boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
