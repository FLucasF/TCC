package shop;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

class OrderService {
    private static final Duration PAYMENT_WINDOW = Duration.ofHours(48);

    private final Store store;
    private final PricingService pricing;
    private final Clock clock;
    private final List<OrderEffect> effects = List.of(new StockEffect(), new ClubCreditEffect(), new NotificationEffect());

    OrderService(Store store, PricingService pricing, Clock clock) {
        this.store = store;
        this.pricing = pricing;
        this.clock = clock;
    }

    Summary summary(CheckoutRequest request) {
        return store.transaction(tx -> pricing.calculate(request, tx));
    }

    Order register(CheckoutRequest request) {
        return store.transaction(tx -> {
            Summary summary = pricing.calculate(request, tx);
            List<OrderItem> items = request.items().stream()
                    .map(i -> new OrderItem(i.sku(), i.quantity())).toList();
            Order order = new Order(UUID.randomUUID().toString(), request.cpf(), request.email(),
                    OrderStatus.AWAITING_PAYMENT, clock.instant().truncatedTo(ChronoUnit.SECONDS), summary, items,
                    null, null, null);
            tx.ensureCustomer(request.cpf(), request.email());
            tx.insertOrder(order);
            return order;
        });
    }

    Order get(String id) {
        return store.transaction(tx -> tx.findOrder(id).orElseThrow(OrderService::notFound));
    }

    /** Resultado de uma transação que pode ter confirmado algo e mesmo assim recusar o pedido. */
    private record Outcome(Order order, ApiException error) {}

    Order pay(String id, String transactionId, String paidAtText) {
        Outcome outcome = store.transaction(tx -> {
            Order order = tx.lockOrder(id).orElseThrow(OrderService::notFound);
            Instant paidAt = parseInstant(paidAtText);
            if (transactionId == null || transactionId.isBlank() || paidAt == null) {
                throw ApiException.unprocessable("INVALID_ORDER");
            }
            PaymentRecord previous = tx.findPayment(id, transactionId).orElse(null);
            if (previous != null) {
                return previous.errorCode() == null
                        ? new Outcome(order, null)
                        : new Outcome(null, new ApiException(previous.errorCode(), previous.httpStatus()));
            }
            if (order.status() != OrderStatus.AWAITING_PAYMENT) throw ApiException.conflict("OPERATION_NOT_ALLOWED");

            if (paidAt.isAfter(order.createdAt().plus(PAYMENT_WINDOW))) {
                return reject(tx, order, transactionId, "ORDER_EXPIRED");
            }
            try {
                for (OrderEffect e : effects) e.onPaid(tx, order);
            } catch (ApiException e) {
                return reject(tx, order, transactionId, e.code());
            }
            Order paid = order.paid(transactionId, paidAt);
            tx.updateOrder(paid);
            tx.savePayment(id, transactionId, new PaymentRecord(null, 200));
            return new Outcome(paid, null);
        });
        if (outcome.error() != null) throw outcome.error();
        return outcome.order();
    }

    private Outcome reject(Tx tx, Order order, String transactionId, String code) {
        cancelInTx(tx, order, false);
        tx.savePayment(order.id(), transactionId, new PaymentRecord(code, 409));
        return new Outcome(null, ApiException.conflict(code));
    }

    Order ship(String id, String trackingCode) {
        return store.transaction(tx -> {
            Order order = tx.lockOrder(id).orElseThrow(OrderService::notFound);
            if (trackingCode == null || trackingCode.isBlank()) throw ApiException.unprocessable("INVALID_ORDER");
            if (order.status() != OrderStatus.PAID) throw ApiException.conflict("OPERATION_NOT_ALLOWED");
            Order shipped = order.shipped(trackingCode);
            tx.updateOrder(shipped);
            for (OrderEffect e : effects) e.onShipped(tx, shipped);
            return shipped;
        });
    }

    Order deliver(String id) {
        return store.transaction(tx -> {
            Order order = tx.lockOrder(id).orElseThrow(OrderService::notFound);
            if (order.status() != OrderStatus.SHIPPED) throw ApiException.conflict("OPERATION_NOT_ALLOWED");
            Order delivered = order.withStatus(OrderStatus.DELIVERED);
            tx.updateOrder(delivered);
            return delivered;
        });
    }

    Order cancel(String id) {
        return store.transaction(tx -> {
            Order order = tx.lockOrder(id).orElseThrow(OrderService::notFound);
            if (order.status() != OrderStatus.AWAITING_PAYMENT && order.status() != OrderStatus.PAID) {
                throw ApiException.conflict("OPERATION_NOT_ALLOWED");
            }
            return cancelInTx(tx, order, order.status() == OrderStatus.PAID);
        });
    }

    private Order cancelInTx(Tx tx, Order order, boolean wasPaid) {
        Order cancelled = order.withStatus(OrderStatus.CANCELLED);
        if (wasPaid) {
            for (OrderEffect e : effects) e.onPaidOrderCancelled(tx, order);
        }
        tx.updateOrder(cancelled);
        for (OrderEffect e : effects) e.onCancelled(tx, cancelled);
        return cancelled;
    }

    private static Instant parseInstant(String text) {
        if (text == null) return null;
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static ApiException notFound() {
        return ApiException.notFound("ORDER_NOT_FOUND");
    }
}
