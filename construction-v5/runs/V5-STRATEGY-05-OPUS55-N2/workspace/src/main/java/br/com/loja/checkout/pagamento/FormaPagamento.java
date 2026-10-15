package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Uma forma de pagamento, com suas regras de parcelamento e ajuste. */
public interface FormaPagamento {

    String codigo();

    boolean aceitaParcelas(int parcelas);

    /** Se a forma de pagamento pode ser usada para este total do pedido. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
