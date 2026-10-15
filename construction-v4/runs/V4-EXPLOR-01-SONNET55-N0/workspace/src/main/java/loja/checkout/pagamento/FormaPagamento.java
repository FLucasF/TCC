package loja.checkout.pagamento;

import java.math.BigDecimal;

public interface FormaPagamento {
    String codigo();

    boolean parcelasPermitidas(int parcelas);

    default boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    Liquidacao liquidar(BigDecimal totalPedido, int parcelas);
}
