package br.com.loja.order;

/**
 * Quem reage ao que acontece com um pedido (crédito, avisos, e no futuro SMS, cashback...). Roda na mesma
 * transação da mudança. Nova reação = novo bean que implemente esta interface.
 */
public interface OrderEventListener {

    default void onPaid(Order order) {
    }

    default void onShipped(Order order) {
    }

    default void onCancelled(Order order, boolean wasPaid) {
    }
}
