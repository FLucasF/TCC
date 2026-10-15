package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Codificavel;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Cada uma decide seu parcelamento permitido,
 * se atende o pedido e qual ajuste aplica sobre o total.
 */
public interface FormaPagamento extends Codificavel {

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);

    boolean aceitaParcelamento(int parcelas);

    /** Por padrao a forma de pagamento atende qualquer pedido. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
