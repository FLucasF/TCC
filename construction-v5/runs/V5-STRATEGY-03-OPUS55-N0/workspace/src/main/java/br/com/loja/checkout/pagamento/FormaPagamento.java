package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Para criar uma nova, basta escrever uma classe que implemente
 * esta interface e anotá-la com {@code @Component}.
 */
public interface FormaPagamento {

    /** Código enviado pelo site em {@code formaPagamento}. */
    String codigo();

    boolean permiteParcelas(int parcelas);

    /** Se a forma de pagamento atende um pedido com este total (ex.: limite do boleto). */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    Pagamento calcular(BigDecimal totalPedido, int parcelas);
}
