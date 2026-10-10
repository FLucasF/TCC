package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Para aceitar uma forma nova, basta criar um {@code @Component}
 * que implemente esta interface.
 */
public interface FormaPagamento {

    String codigo();

    /** Se o número de parcelas é permitido. Por padrão, só à vista. */
    default boolean parcelasPermitidas(int parcelas) {
        return parcelas == 1;
    }

    /** Se a forma atende um pedido com este total (antes do ajuste do pagamento). */
    default boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    Pagamento pagar(BigDecimal totalPedido, int parcelas);
}
