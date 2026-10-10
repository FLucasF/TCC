package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Identificado;

import java.math.BigDecimal;

/**
 * Uma forma de pagar: em quantas vezes ela aceita, quais totais ela atende e
 * quanto o cliente paga no fim.
 */
public interface FormaPagamento extends Identificado {

    ResultadoPagamento liquidar(BigDecimal totalPedido, int parcelas);

    default boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    default boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
