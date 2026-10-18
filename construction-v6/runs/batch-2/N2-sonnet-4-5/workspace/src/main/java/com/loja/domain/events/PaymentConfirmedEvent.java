package com.loja.domain.events;

import com.loja.domain.entities.Order;

public class PaymentConfirmedEvent {
    private final Order order;

    public PaymentConfirmedEvent(Order order) {
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }
}
