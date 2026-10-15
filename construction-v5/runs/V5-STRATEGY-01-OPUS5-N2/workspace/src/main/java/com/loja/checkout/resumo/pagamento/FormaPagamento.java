package com.loja.checkout.resumo.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento: quantas parcelas aceita, quando atende o pedido e
 * como transforma o total do pedido no valor final.
 */
public interface FormaPagamento {

    Parcelamento calcular(BigDecimal totalPedido, int parcelas);

    boolean permiteParcelas(int parcelas);

    /** Se a forma de pagamento atende um pedido com este total. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
