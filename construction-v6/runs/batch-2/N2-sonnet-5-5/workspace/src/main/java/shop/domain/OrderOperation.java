package shop.domain;

import java.util.Set;
import static shop.domain.OrderStatus.*;

/** Transições permitidas. Uma nova situação (ex.: em separação) entra aqui. */
public enum OrderOperation {
    PAY(Set.of(AWAITING_PAYMENT), PAID),
    SHIP(Set.of(PAID), SHIPPED),
    DELIVER(Set.of(SHIPPED), DELIVERED),
    CANCEL(Set.of(AWAITING_PAYMENT, PAID), CANCELLED);

    private final Set<OrderStatus> allowedFrom;
    private final OrderStatus target;

    OrderOperation(Set<OrderStatus> allowedFrom, OrderStatus target) {
        this.allowedFrom = allowedFrom;
        this.target = target;
    }

    public OrderStatus target() {
        return target;
    }

    public void requireAllowed(OrderStatus current) {
        if (!allowedFrom.contains(current)) {
            throw new DomainException(ErrorCode.OPERATION_NOT_ALLOWED);
        }
    }
}
