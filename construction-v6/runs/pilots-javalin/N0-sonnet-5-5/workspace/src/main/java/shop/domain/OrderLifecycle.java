package shop.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static shop.domain.OrderStatus.*;

/** The only place that says which operation is allowed in which status. */
public final class OrderLifecycle {
    public enum Operation { PAY, SHIP, DELIVER, CANCEL }

    private static final Map<Operation, Map<OrderStatus, OrderStatus>> TRANSITIONS = new EnumMap<>(Operation.class);

    static {
        TRANSITIONS.put(Operation.PAY, Map.of(AWAITING_PAYMENT, PAID));
        TRANSITIONS.put(Operation.SHIP, Map.of(PAID, SHIPPED));
        TRANSITIONS.put(Operation.DELIVER, Map.of(SHIPPED, DELIVERED));
        TRANSITIONS.put(Operation.CANCEL, Map.of(AWAITING_PAYMENT, CANCELLED, PAID, CANCELLED));
    }

    private OrderLifecycle() {
    }

    public static Optional<OrderStatus> next(OrderStatus current, Operation operation) {
        return Optional.ofNullable(TRANSITIONS.get(operation).get(current));
    }

    /** Returns the next status or refuses with OPERATION_NOT_ALLOWED. */
    public static OrderStatus require(OrderStatus current, Operation operation) {
        return next(current, operation).orElseThrow(() -> ApiException.conflict("OPERATION_NOT_ALLOWED"));
    }
}
