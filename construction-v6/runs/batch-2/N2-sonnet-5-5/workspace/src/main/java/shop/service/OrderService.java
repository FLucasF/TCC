package shop.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import shop.domain.DomainException;
import shop.domain.ErrorCode;
import shop.domain.Order;
import shop.domain.OrderOperation;
import shop.domain.OrderStatus;
import shop.persistence.PaymentRepository;
import shop.persistence.Store;
import shop.persistence.Tx;

public class OrderService {
    private static final Duration PAYMENT_WINDOW = Duration.ofHours(48);

    private final Store store;
    private final List<OrderEventListener> listeners;

    public OrderService(Store store, List<OrderEventListener> listeners) {
        this.store = store;
        this.listeners = listeners;
    }

    public Order get(String id) {
        return store.inTransaction(tx -> tx.orders().find(id).orElseThrow(OrderService::notFound));
    }

    private record PaymentOutcome(Order order, ErrorCode error) {
    }

    /** transactionId e paidAt podem ser nulos (ausentes): a ordem das validações é decidida aqui. */
    public Order confirmPayment(String id, String transactionId, Instant paidAt) {
        PaymentOutcome outcome = store.inTransaction(tx -> {
            Order order = tx.orders().findForUpdate(id).orElseThrow(OrderService::notFound);
            if (transactionId == null || transactionId.isBlank() || paidAt == null) {
                throw new DomainException(ErrorCode.INVALID_ORDER);
            }
            PaymentRepository.PaymentRecord previous = tx.payments().find(id, transactionId).orElse(null);
            if (previous != null) {
                return new PaymentOutcome(order, previous.errorCode() == null ? null
                        : ErrorCode.valueOf(previous.errorCode()));
            }
            OrderOperation.PAY.requireAllowed(order.status());
            if (Duration.between(order.createdAt(), paidAt).compareTo(PAYMENT_WINDOW) > 0) {
                return refuse(tx, order, transactionId, ErrorCode.ORDER_EXPIRED);
            }
            try {
                listeners.forEach(l -> l.onPaid(tx, order));
            } catch (StockShortageException e) {
                return refuse(tx, order, transactionId, ErrorCode.INSUFFICIENT_STOCK);
            }
            Order paid = order.withStatus(OrderOperation.PAY.target());
            tx.orders().update(paid);
            tx.payments().record(id, transactionId, null);
            return new PaymentOutcome(paid, null);
        });
        if (outcome.error() != null) {
            throw DomainException.conflict(outcome.error());
        }
        return outcome.order();
    }

    private PaymentOutcome refuse(Tx tx, Order order, String transactionId, ErrorCode error) {
        cancelOrder(tx, order, false);
        tx.payments().record(order.id(), transactionId, error.name());
        return new PaymentOutcome(order.withStatus(OrderStatus.CANCELLED), error);
    }

    public Order ship(String id, String trackingCode) {
        return store.inTransaction(tx -> {
            Order order = tx.orders().findForUpdate(id).orElseThrow(OrderService::notFound);
            if (trackingCode == null || trackingCode.isBlank()) {
                throw new DomainException(ErrorCode.INVALID_ORDER);
            }
            OrderOperation.SHIP.requireAllowed(order.status());
            Order shipped = order.withShipment(OrderOperation.SHIP.target(), trackingCode);
            tx.orders().update(shipped);
            listeners.forEach(l -> l.onShipped(tx, shipped));
            return shipped;
        });
    }

    public Order deliver(String id) {
        return store.inTransaction(tx -> {
            Order order = tx.orders().findForUpdate(id).orElseThrow(OrderService::notFound);
            OrderOperation.DELIVER.requireAllowed(order.status());
            Order delivered = order.withStatus(OrderOperation.DELIVER.target());
            tx.orders().update(delivered);
            return delivered;
        });
    }

    public Order cancel(String id) {
        return store.inTransaction(tx -> {
            Order order = tx.orders().findForUpdate(id).orElseThrow(OrderService::notFound);
            OrderOperation.CANCEL.requireAllowed(order.status());
            return cancelOrder(tx, order, order.status() == OrderStatus.PAID);
        });
    }

    private Order cancelOrder(Tx tx, Order order, boolean wasPaid) {
        Order cancelled = order.withStatus(OrderOperation.CANCEL.target());
        tx.orders().update(cancelled);
        listeners.forEach(l -> l.onCancelled(tx, cancelled, wasPaid));
        return cancelled;
    }

    private static DomainException notFound() {
        return new DomainException(ErrorCode.ORDER_NOT_FOUND);
    }
}
