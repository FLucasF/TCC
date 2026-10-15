package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento aceita pela loja.
 *
 * <p>Para aceitar uma forma nova basta criar uma classe que implemente esta interface e anota-la
 * com {@code @Component}.
 */
public interface FormaPagamento {

    /** Codigo enviado pelo site, em letras maiusculas (ex.: PIX). */
    String codigo();

    /** Se a forma de pagamento aceita esse numero de parcelas. */
    default boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    /** Se a forma de pagamento atende este pedido (ex.: teto de valor do boleto). */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    /** Aplica o ajuste da forma de pagamento sobre o total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
