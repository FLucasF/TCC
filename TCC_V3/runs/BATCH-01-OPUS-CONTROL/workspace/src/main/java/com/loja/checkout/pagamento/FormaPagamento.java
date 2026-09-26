package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento aceita pela loja.
 *
 * <p>Para aceitar uma forma nova basta implementar esta interface e anotar a classe
 * com {@code @Component}.
 */
public interface FormaPagamento {

    /** Codigo usado pelo site (ex.: PIX). */
    String codigo();

    /** Diz se a loja aceita esse numero de parcelas nessa forma de pagamento. */
    boolean aceitaParcelas(int parcelas);

    /** Diz se a forma de pagamento atende um pedido desse valor. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    /** Calcula o valor final e o valor da parcela a partir do total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
