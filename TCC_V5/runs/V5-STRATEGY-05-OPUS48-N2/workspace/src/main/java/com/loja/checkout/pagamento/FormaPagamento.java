package com.loja.checkout.pagamento;

import com.loja.checkout.comum.Codificavel;

import java.math.BigDecimal;

/**
 * Forma de pagamento. Cada forma tem seu ajuste sobre o total do pedido, seu
 * número de parcelas permitido e sua condição de disponibilidade, então cada
 * uma mora numa classe própria.
 */
public interface FormaPagamento extends Codificavel {

    /** Número de parcelas permitido (Pix e boleto só 1; cartão de 1 a 12). */
    boolean parcelasPermitidas(int parcelas);

    /** A forma atende este total? (ex.: boleto só até R$ 1.000,00) */
    boolean atende(BigDecimal totalPedido);

    /** Valor final e valor da parcela. Só chamado quando permitido e atende. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
