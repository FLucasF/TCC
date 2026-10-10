package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.Codificado;
import java.math.BigDecimal;

/** Uma forma de pagamento. Para incluir uma nova, basta criar outro componente que implemente esta interface. */
public interface FormaPagamento extends Codificado {

    boolean permiteParcelas(int parcelas);

    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    Pagamento pagar(BigDecimal totalPedido, int parcelas);
}
