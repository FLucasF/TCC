package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Cada forma de pagamento define seu parcelamento, suas restrições e seu ajuste. */
public interface FormaPagamento {

    String codigo();

    boolean permiteParcelas(int parcelas);

    Pagamento pagar(BigDecimal totalPedido, int parcelas);

    default boolean atende(BigDecimal totalPedido) {
        return true;
    }
}
