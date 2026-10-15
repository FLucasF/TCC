package loja.checkout.pagamento;

import java.math.BigDecimal;

import loja.checkout.Codificado;

public interface FormaPagamento extends Codificado {

    boolean aceitaParcelas(int parcelas);

    /** Se a forma de pagamento atende um pedido com este total. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
