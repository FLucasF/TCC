package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Codificado;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Cada forma ajusta o total do pedido de um jeito
 * (desconto do Pix, tarifa do boleto, juros do cartão) e tem suas regras de
 * parcelamento e disponibilidade, então cada uma mora na sua própria classe.
 */
public interface FormaPagamento extends Codificado {

    /** Se o número de parcelas é permitido para esta forma. */
    boolean parcelasPermitidas(int parcelas);

    /** Se a forma atende um pedido com este total (ex.: boleto até R$ 1.000). */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    /** Valor final e valor de cada parcela, a partir do total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
