package br.com.loja.pedidos.pedido;

/**
 * Algo que acontece quando o pagamento de um pedido é confirmado (e se desfaz se o pedido pago for cancelado). Para
 * acrescentar um novo efeito, como SMS ou pontos de cashback, basta criar um componente que implemente esta interface.
 * Roda na mesma transação da confirmação.
 */
public interface EfeitoPagamento {

    void aoConfirmar(Pedido pedido);

    default void aoCancelarPedidoPago(Pedido pedido) {
    }
}
