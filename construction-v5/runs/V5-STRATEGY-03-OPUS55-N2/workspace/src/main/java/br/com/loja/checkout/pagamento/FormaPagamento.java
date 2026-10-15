package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Uma forma de pagamento e o ajuste que ela aplica sobre o total do pedido. */
public interface FormaPagamento {

    String codigo();

    boolean aceitaParcelas(int parcelas);

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);

    default boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
