package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Codificado;

import java.math.BigDecimal;

/** Uma forma de pagamento: seu ajuste sobre o total, seu parcelamento e suas limitacoes. */
public interface FormaPagamento extends Codificado {

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);

    boolean permiteParcelas(int parcelas);

    /** Se a forma de pagamento atende este pedido. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
