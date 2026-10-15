package com.loja.orders.order;

/**
 * O que mais precisa acontecer quando um pedido muda de situação (estoque, crédito do clube, avisos
 * ao cliente...). Para uma reação nova, como SMS ou cashback, basta um novo {@code @Component} que
 * implemente esta interface. Roda na mesma transação da mudança de situação.
 */
public interface OrderLifecycleListener {

    default void paymentApproved(Order order) {
    }

    default void shipped(Order order) {
    }

    default void cancelled(Order order, OrderStatus previousStatus) {
    }
}
