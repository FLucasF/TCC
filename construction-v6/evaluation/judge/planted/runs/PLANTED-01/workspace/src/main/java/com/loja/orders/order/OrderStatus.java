package com.loja.orders.order;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** As situações do pedido e para onde cada uma pode ir. */
public enum OrderStatus {
    AWAITING_PAYMENT,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> NEXT = new EnumMap<>(OrderStatus.class);

    static {
        NEXT.put(AWAITING_PAYMENT, EnumSet.of(PAID, CANCELLED));
        NEXT.put(PAID, EnumSet.of(SHIPPED, CANCELLED));
        NEXT.put(SHIPPED, EnumSet.of(DELIVERED));
        NEXT.put(DELIVERED, EnumSet.noneOf(OrderStatus.class));
        NEXT.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    public boolean canMoveTo(OrderStatus next) {
        return NEXT.get(this).contains(next);
    }

    /** Se o pagamento deste pedido foi aceito e não foi desfeito (conta como compra feita). */
    public boolean paymentSettled() {
        return this == PAID || this == SHIPPED || this == DELIVERED;
    }
}
