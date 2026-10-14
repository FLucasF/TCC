package br.com.loja.pedidos.pagamento;

import java.math.BigDecimal;

/** Uma forma de pagamento. Para criar uma nova, basta criar um componente que implemente esta interface. */
public interface FormaPagamento {

    String codigo();

    /** Se a forma aceita pagar neste número de parcelas. */
    default boolean aceitaParcelas(int parcelas) {
        return parcelas == 1;
    }

    /** Se a forma atende um pedido com este total. */
    default boolean atende(BigDecimal totalPedido) {
        return true;
    }

    /** Aplica o ajuste da forma de pagamento (desconto, tarifa ou juros) sobre o total do pedido. */
    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas, boolean usouCupom);
}
