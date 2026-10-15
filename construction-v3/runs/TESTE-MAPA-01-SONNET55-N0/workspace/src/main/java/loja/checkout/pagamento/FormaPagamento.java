package loja.checkout.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    String codigo();

    boolean parcelasPermitidas(int parcelas);

    default boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
