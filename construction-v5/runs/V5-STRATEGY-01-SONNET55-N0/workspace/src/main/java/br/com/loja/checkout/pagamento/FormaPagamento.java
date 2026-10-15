package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Para uma nova forma de pagamento, basta criar um @Component que implemente esta interface. */
public interface FormaPagamento {

    String codigo();

    boolean parcelasPermitidas(int parcelas);

    default boolean disponivel(BigDecimal totalPedido) {
        return true;
    }

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
