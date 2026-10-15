package br.com.loja.pedidos.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Para criar uma forma nova, basta um novo componente do Spring que
 * implemente esta interface.
 */
public interface FormaPagamento {

    /** Código usado no campo {@code formaPagamento}. */
    String codigo();

    boolean aceitaParcelas(int parcelas);

    /** Se a forma de pagamento atende um pedido com este total. */
    default boolean disponivelPara(BigDecimal totalPedido) {
        return true;
    }

    /** Valor final e parcelas, aplicando o ajuste da forma de pagamento sobre o total do pedido. */
    CondicaoPagamento condicao(BigDecimal totalPedido, int parcelas, boolean usouCupom);
}
