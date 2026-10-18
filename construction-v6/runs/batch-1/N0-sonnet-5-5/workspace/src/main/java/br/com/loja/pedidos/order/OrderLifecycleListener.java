package br.com.loja.pedidos.order;

/** Reacts to order events, inside the transaction that changes the order. */
public interface OrderLifecycleListener {

    default void onPaid(StoredOrder order) {
    }

    default void onShipped(StoredOrder order) {
    }

    default void onCancelled(StoredOrder order, boolean wasPaid) {
    }
}
