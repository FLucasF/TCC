package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Cada uma tem seu ajuste sobre o total do pedido,
 * seus parcelamentos permitidos e suas restricoes.
 */
public interface FormaPagamento {

    String codigo();

    /** Se o numero de parcelas e permitido nesta forma. */
    boolean permiteParcelas(int parcelas);

    /** Se a forma atende este pedido (ex.: boleto so ate R$ 1.000,00). */
    boolean atende(BigDecimal totalPedido);

    Liquidacao liquidar(BigDecimal totalPedido, int parcelas);
}
