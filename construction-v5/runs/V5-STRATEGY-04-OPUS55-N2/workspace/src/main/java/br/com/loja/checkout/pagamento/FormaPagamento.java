package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Uma forma de pagamento: quais parcelamentos aceita, quando atende e como ajusta o total. */
public interface FormaPagamento {

    String codigo();

    boolean aceitaParcelas(int parcelas);

    /** Se a forma de pagamento atende um pedido com este total. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
