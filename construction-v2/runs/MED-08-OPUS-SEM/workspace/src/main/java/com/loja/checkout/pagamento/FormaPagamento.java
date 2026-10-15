package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento aceita pela loja.
 *
 * <p>Forma nova e uma classe nova anotada com {@code @Component}; o catalogo se monta sozinho.
 */
public interface FormaPagamento {

    /** Codigo usado pelo site, em letras maiusculas (ex.: {@code PIX}). */
    String codigo();

    /** {@code true} quando esse numero de parcelas e permitido nessa forma de pagamento. */
    default boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    /** {@code false} quando a forma existe mas nao atende um pedido com esse total. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    /** Aplica o ajuste da forma de pagamento sobre o total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
