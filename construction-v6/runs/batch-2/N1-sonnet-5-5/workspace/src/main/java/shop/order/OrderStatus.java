package shop.order;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    AWAITING_PAYMENT(false),
    PAID(true),
    SHIPPED(true),
    DELIVERED(true),
    CANCELLED(false);

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        TRANSITIONS.put(AWAITING_PAYMENT, EnumSet.of(PAID, CANCELLED));
        TRANSITIONS.put(PAID, EnumSet.of(SHIPPED, CANCELLED));
        TRANSITIONS.put(SHIPPED, EnumSet.of(DELIVERED));
        TRANSITIONS.put(DELIVERED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    private final boolean paymentConfirmed;

    OrderStatus(boolean paymentConfirmed) {
        this.paymentConfirmed = paymentConfirmed;
    }

    public boolean canMoveTo(OrderStatus target) {
        return TRANSITIONS.get(this).contains(target);
    }

    /** Whether stock, credit and the other payment effects have already been applied. */
    public boolean paymentConfirmed() {
        return paymentConfirmed;
    }
}
