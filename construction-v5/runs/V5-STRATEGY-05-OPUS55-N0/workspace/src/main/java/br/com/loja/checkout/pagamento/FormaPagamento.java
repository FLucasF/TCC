package br.com.loja.checkout.pagamento;

import br.com.loja.checkout.dominio.Codificado;
import java.math.BigDecimal;

/** Uma forma de pagamento. Para criar uma nova, crie uma classe {@code @Component} que implemente esta interface. */
public interface FormaPagamento extends Codificado {

    /** Se o número de parcelas é permitido nesta forma de pagamento. */
    boolean aceitaParcelas(int parcelas);

    /** Se esta forma de pagamento aceita um pedido com este total. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
