package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Para aceitar uma forma nova, crie uma classe
 * {@code @Component} que implemente esta interface.
 */
public interface FormaPagamento {

    String codigo();

    /** Se o número de parcelas é permitido. Por padrão, só à vista. */
    default boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    /** Se a forma de pagamento atende um pedido com este total. */
    default boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    ResultadoPagamento pagar(BigDecimal totalPedido, int parcelas);
}
