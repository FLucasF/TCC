package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.catalogo.Codificado;
import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Para aceitar uma forma nova, crie um {@code @Component}
 * que implemente esta interface.
 */
public interface FormaPagamento extends Codificado {

    /** Se o número de parcelas é permitido. Por padrão, só à vista. */
    default boolean permiteParcelas(int parcelas) {
        return parcelas == 1;
    }

    /** Se a forma de pagamento aceita um pedido com este total. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
