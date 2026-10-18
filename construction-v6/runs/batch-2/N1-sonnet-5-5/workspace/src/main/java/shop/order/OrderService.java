package shop.order;

import com.fasterxml.jackson.databind.JsonNode;
import shop.ApiException;
import shop.checkout.CheckoutCalculator;
import shop.checkout.CheckoutRequest;
import shop.checkout.Summary;
import shop.notification.NotificationType;
import shop.notification.Notifier;
import shop.persistence.Store;
import shop.persistence.Tx;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class OrderService {
    private static final Duration PAYMENT_WINDOW = Duration.ofHours(48);

    private final Store store;
    private final CheckoutCalculator calculator;
    private final Notifier notifier;
    private final List<PaymentConfirmationReaction> reactions;
    private final Clock clock;

    public OrderService(Store store, CheckoutCalculator calculator, Notifier notifier,
                        List<PaymentConfirmationReaction> reactions, Clock clock) {
        this.store = store;
        this.calculator = calculator;
        this.notifier = notifier;
        this.reactions = reactions;
        this.clock = clock;
    }

    public Summary summary(CheckoutRequest request) {
        return store.inTransaction(tx -> calculator.calculate(tx, request));
    }

    public Order register(CheckoutRequest request) {
        return store.inTransaction(tx -> {
            Summary summary = calculator.calculate(tx, request);
            tx.customers().createIfAbsent(request.cpf());
            List<OrderItem> items = request.items().stream()
                    .map(item -> new OrderItem(item.sku(), item.quantity())).toList();
            Order order = new Order(UUID.randomUUID().toString(), request.cpf(), request.email(),
                    OrderStatus.AWAITING_PAYMENT, clock.instant().truncatedTo(ChronoUnit.SECONDS), summary, items, null);
            tx.orders().insert(order);
            return order;
        });
    }

    public Order get(String id) {
        return store.inTransaction(tx -> tx.orders().find(id).orElseThrow(OrderService::notFound));
    }

    private record PaymentResult(Order order, String refusal) {
    }

    public Order confirmPayment(String id, JsonNode body) {
        PaymentResult result = store.inTransaction(tx -> {
            Order order = tx.orders().lock(id).orElseThrow(OrderService::notFound);
            String transactionId = CheckoutRequest.text(body, "transactionId");
            String paidAtText = CheckoutRequest.text(body, "paidAt");
            if (transactionId == null || transactionId.isBlank() || paidAtText == null) {
                throw ApiException.unprocessable("INVALID_ORDER");
            }
            Instant paidAt;
            try {
                paidAt = Instant.parse(paidAtText);
            } catch (DateTimeParseException e) {
                throw ApiException.unprocessable("INVALID_ORDER");
            }
            var previous = tx.paymentNotices().find(id, transactionId);
            if (previous.isPresent()) {
                return new PaymentResult(order, previous.get().refusalCode());
            }
            requireTransition(order, OrderStatus.PAID);
            if (Duration.between(order.createdAt(), paidAt).compareTo(PAYMENT_WINDOW) > 0) {
                return refuse(tx, order, transactionId, "ORDER_EXPIRED");
            }
            if (!reserveStock(tx, order)) {
                return refuse(tx, order, transactionId, "INSUFFICIENT_STOCK");
            }
            Order paid = order.withStatus(OrderStatus.PAID);
            tx.orders().update(paid);
            reactions.forEach(reaction -> reaction.onPaid(tx, paid));
            tx.paymentNotices().insert(new PaymentNotice(id, transactionId, null));
            return new PaymentResult(paid, null);
        });
        if (result.refusal() != null) {
            throw ApiException.conflict(result.refusal());
        }
        return result.order();
    }

    public Order ship(String id, JsonNode body) {
        return store.inTransaction(tx -> {
            Order order = tx.orders().lock(id).orElseThrow(OrderService::notFound);
            String trackingCode = CheckoutRequest.text(body, "trackingCode");
            if (trackingCode == null || trackingCode.isBlank()) {
                throw ApiException.unprocessable("INVALID_ORDER");
            }
            requireTransition(order, OrderStatus.SHIPPED);
            Order shipped = order.withStatus(OrderStatus.SHIPPED).withTrackingCode(trackingCode);
            tx.orders().update(shipped);
            notifier.notify(tx, shipped, NotificationType.ORDER_SHIPPED);
            return shipped;
        });
    }

    public Order deliver(String id) {
        return store.inTransaction(tx -> {
            Order order = tx.orders().lock(id).orElseThrow(OrderService::notFound);
            requireTransition(order, OrderStatus.DELIVERED);
            Order delivered = order.withStatus(OrderStatus.DELIVERED);
            tx.orders().update(delivered);
            return delivered;
        });
    }

    public Order cancel(String id) {
        return store.inTransaction(tx -> {
            Order order = tx.orders().lock(id).orElseThrow(OrderService::notFound);
            requireTransition(order, OrderStatus.CANCELLED);
            return cancelOrder(tx, order);
        });
    }

    private PaymentResult refuse(Tx tx, Order order, String transactionId, String code) {
        Order cancelled = cancelOrder(tx, order);
        tx.paymentNotices().insert(new PaymentNotice(order.id(), transactionId, code));
        return new PaymentResult(cancelled, code);
    }

    /** Cancels the order, undoing what the payment did when it had been confirmed. */
    private Order cancelOrder(Tx tx, Order order) {
        if (order.status().paymentConfirmed()) {
            releaseStock(tx, order);
            reactions.forEach(reaction -> reaction.onPaidOrderCancelled(tx, order));
        }
        Order cancelled = order.withStatus(OrderStatus.CANCELLED);
        tx.orders().update(cancelled);
        notifier.notify(tx, cancelled, NotificationType.ORDER_CANCELLED);
        return cancelled;
    }

    /** Locks the products (always in SKU order) and takes the stock only if every item is covered. */
    private boolean reserveStock(Tx tx, Order order) {
        List<OrderItem> items = sorted(order);
        for (OrderItem item : items) {
            boolean enough = tx.products().lock(item.sku()).map(product -> product.stock() >= item.quantity()).orElse(false);
            if (!enough) {
                return false;
            }
        }
        items.forEach(item -> tx.products().adjustStock(item.sku(), -item.quantity()));
        return true;
    }

    private void releaseStock(Tx tx, Order order) {
        sorted(order).forEach(item -> tx.products().adjustStock(item.sku(), item.quantity()));
    }

    private static List<OrderItem> sorted(Order order) {
        return order.items().stream().sorted(Comparator.comparing(OrderItem::sku)).toList();
    }

    private static void requireTransition(Order order, OrderStatus target) {
        if (!order.status().canMoveTo(target)) {
            throw ApiException.conflict("OPERATION_NOT_ALLOWED");
        }
    }

    private static ApiException notFound() {
        return ApiException.notFound("ORDER_NOT_FOUND");
    }
}
