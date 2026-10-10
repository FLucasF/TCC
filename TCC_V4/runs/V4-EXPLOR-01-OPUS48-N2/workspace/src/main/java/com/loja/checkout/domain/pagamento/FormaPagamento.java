package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Cada forma tem seu ajuste sobre o total, seu
 * parcelamento permitido e suas limitações, então cada uma mora numa
 * implementação própria.
 */
public interface FormaPagamento {

    String codigo();

    /** Se a forma aceita esse número de parcelas. */
    boolean parcelasPermitidas(int parcelas);

    /** Se a forma atende um pedido com este total. Por padrão, atende. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    /** Fecha o pedido: valor final e valor de cada parcela. */
    PagamentoResultado calcular(BigDecimal totalPedido, int parcelas);
}
