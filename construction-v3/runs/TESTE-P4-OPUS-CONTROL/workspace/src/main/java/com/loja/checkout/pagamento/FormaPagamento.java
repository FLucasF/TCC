package com.loja.checkout.pagamento;

/**
 * Forma de pagamento aceita pela loja. Para aceitar uma nova basta uma
 * implementacao anotada com {@code @Component}.
 */
public interface FormaPagamento {

    /** Codigo enviado pelo site, em maiusculas (ex.: PIX). */
    String codigo();

    /** Se a forma aceita esse numero de parcelas. */
    default boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    /** Se a forma atende este pedido (valor maximo, etc.). */
    default boolean disponivel(ContextoPagamento contexto) {
        return true;
    }

    /** Total final e valor da parcela. */
    ResultadoPagamento calcular(ContextoPagamento contexto);
}
