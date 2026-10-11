package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Para criar uma nova, basta implementar esta interface
 * em uma classe anotada com {@code @Component}.
 */
public interface FormaPagamento {

    String codigo();

    boolean parcelamentoPermitido(int parcelas);

    /** Se a forma de pagamento aceita um pedido com este total. */
    default boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
