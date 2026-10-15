package com.loja.pedidos.pagamento;

import java.math.BigDecimal;

/** Uma forma de pagamento. Nova forma = novo bean. */
public interface FormaPagamento {

    String codigo();

    /** Lança PARCELAMENTO_INVALIDO se o número de parcelas não é permitido. */
    void validarParcelas(int parcelas);

    /** Lança FORMA_PAGAMENTO_INDISPONIVEL se a forma não atende o pedido. */
    default void validarDisponibilidade(BigDecimal totalPedido) {
    }

    Cobranca cobrar(BigDecimal totalPedido, int parcelas, boolean comCupom);
}
