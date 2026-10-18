package br.com.loja.pedidos.order;

/**
 * Consequências dos acontecimentos do pedido, executadas na mesma transação.
 * Uma consequência nova (SMS, cashback...) entra como uma nova implementação.
 */
public interface OrderEventListener {

    default void paymentConfirmed(Order order) {
    }

    default void shipped(Order order) {
    }

    /** @param wasPaid o pagamento já tinha sido confirmado quando o pedido foi cancelado */
    default void cancelled(Order order, boolean wasPaid) {
    }
}
