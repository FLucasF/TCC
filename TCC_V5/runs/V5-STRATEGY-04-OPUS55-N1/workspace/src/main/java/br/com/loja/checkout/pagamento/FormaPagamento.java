package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.Codificado;

import java.math.BigDecimal;

/** Uma forma de pagamento. Para incluir uma nova, basta criar outra implementação anotada com @Component. */
public interface FormaPagamento extends Codificado {

    boolean permiteParcelas(int parcelas);

    /** Se esta forma de pagamento aceita um pedido com este total. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    Cobranca cobrar(BigDecimal totalPedido, int parcelas);
}
