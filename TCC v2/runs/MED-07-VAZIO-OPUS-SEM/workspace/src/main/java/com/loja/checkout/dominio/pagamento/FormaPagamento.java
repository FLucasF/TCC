package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento aceita pela loja.
 *
 * Forma nova = classe nova anotada com @Component implementando esta interface.
 */
public interface FormaPagamento {

    /** Codigo recebido no campo "formaPagamento" (ex.: PIX). */
    String codigo();

    /** Se a quantidade de parcelas e permitida nesta forma de pagamento. */
    boolean aceitaParcelas(int parcelas);

    /** Se a forma de pagamento atende este pedido (ex.: boleto so ate R$ 1.000,00). */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    /** Calcula o total final e o valor da parcela, ja arredondados para centavos. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
