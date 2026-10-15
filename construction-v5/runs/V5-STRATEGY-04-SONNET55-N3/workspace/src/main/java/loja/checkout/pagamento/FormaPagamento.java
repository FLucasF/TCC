package loja.checkout.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {

    String codigo();

    boolean parcelasPermitidas(int parcelas);

    /** Se a forma de pagamento aceita um pedido com este total. */
    default boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);

    record Cobranca(BigDecimal totalFinal, BigDecimal valorParcela) {
    }
}
