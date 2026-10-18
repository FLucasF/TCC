package shop.app;

import com.fasterxml.jackson.databind.JsonNode;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import shop.db.Dao;
import shop.db.Database;
import shop.domain.ApiException;
import shop.domain.NotificationType;
import shop.domain.Order;
import shop.domain.OrderItem;
import shop.domain.OrderLifecycle;
import shop.domain.OrderLifecycle.Operation;
import shop.domain.OrderStatus;
import shop.domain.Product;

public final class OrderService {
    private static final Duration PAYMENT_WINDOW = Duration.ofHours(48);
    private static final String OK = "OK";
    private static final String EXPIRED = "ORDER_EXPIRED";
    private static final String NO_STOCK = "INSUFFICIENT_STOCK";

    /** Result of a payment notice: the order, or the code of a refusal that was nevertheless persisted. */
    public record PaymentResult(Order order, String error) {
    }

    private final Database db;
    private final CheckoutService checkout;
    private final Clock clock;
    private final Notifier notifier;
    private final List<PaidOrderEffect> effects;

    public OrderService(Database db, CheckoutService checkout, Clock clock, Notifier notifier,
                        List<PaidOrderEffect> effects) {
        this.db = db;
        this.checkout = checkout;
        this.clock = clock;
        this.notifier = notifier;
        this.effects = effects;
    }

    public Order register(JsonNode body) {
        CheckoutService.Checkout c = checkout.prepare(body);
        Order order = new Order(UUID.randomUUID().toString(), c.cpf(), c.email(), OrderStatus.AWAITING_PAYMENT,
                clock.instant().truncatedTo(ChronoUnit.SECONDS), c.summary(), c.items(), null);
        return db.tx(d -> {
            d.ensureCustomer(c.cpf(), c.email());
            d.insertOrder(order);
            return order;
        });
    }

    public Order get(String id) {
        return db.tx(d -> d.order(id, false).orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND")));
    }

    public PaymentResult pay(String id, JsonNode body) {
        return db.tx(d -> {
            Order order = lock(d, id);
            String transactionId = CheckoutService.text(body.path("transactionId"));
            Instant paidAt = parseInstant(body.path("paidAt"));
            if (transactionId == null || paidAt == null) {
                throw ApiException.unprocessable("INVALID_ORDER");
            }
            var previous = d.paymentOutcome(id, transactionId);
            if (previous.isPresent()) {
                return result(order, previous.get());
            }
            OrderLifecycle.require(order.status(), Operation.PAY);

            if (paidAt.isAfter(order.createdAt().plus(PAYMENT_WINDOW))) {
                return refuse(d, order, transactionId, EXPIRED);
            }
            Map<String, Product> products = d.lockProducts(order.items().stream().map(OrderItem::sku).toList());
            for (OrderItem item : order.items()) {
                Product p = products.get(item.sku());
                if (p == null || p.stock() < item.quantity()) {
                    return refuse(d, order, transactionId, NO_STOCK);
                }
            }
            for (OrderItem item : order.items()) {
                d.adjustStock(item.sku(), -item.quantity());
            }
            d.updateStatus(id, OrderStatus.PAID);
            Order paid = withStatus(order, OrderStatus.PAID);
            for (PaidOrderEffect effect : effects) {
                effect.onPaid(d, paid);
            }
            d.recordPaymentOutcome(id, transactionId, OK);
            return new PaymentResult(paid, null);
        });
    }

    public Order ship(String id, JsonNode body) {
        return db.tx(d -> {
            Order order = lock(d, id);
            String trackingCode = CheckoutService.text(body.path("trackingCode"));
            if (trackingCode == null) {
                throw ApiException.unprocessable("INVALID_ORDER");
            }
            OrderStatus next = OrderLifecycle.require(order.status(), Operation.SHIP);
            d.updateStatus(id, next);
            d.updateTrackingCode(id, trackingCode);
            Order shipped = new Order(order.id(), order.cpf(), order.email(), next, order.createdAt(),
                    order.summary(), order.items(), trackingCode);
            notifier.notify(d, shipped, NotificationType.ORDER_SHIPPED);
            return shipped;
        });
    }

    public Order deliver(String id) {
        return db.tx(d -> {
            Order order = lock(d, id);
            OrderStatus next = OrderLifecycle.require(order.status(), Operation.DELIVER);
            d.updateStatus(id, next);
            return withStatus(order, next);
        });
    }

    public Order cancel(String id) {
        return db.tx(d -> {
            Order order = lock(d, id);
            OrderStatus next = OrderLifecycle.require(order.status(), Operation.CANCEL);
            if (order.status() == OrderStatus.PAID) {
                Map<String, Product> locked = d.lockProducts(order.items().stream().map(OrderItem::sku).toList());
                for (OrderItem item : order.items()) {
                    if (locked.containsKey(item.sku())) {
                        d.adjustStock(item.sku(), item.quantity());
                    }
                }
                for (PaidOrderEffect effect : effects) {
                    effect.onPaidOrderCancelled(d, order);
                }
            }
            return markCancelled(d, order, next);
        });
    }

    private PaymentResult refuse(Dao d, Order order, String transactionId, String code) throws SQLException {
        Order cancelled = markCancelled(d, order, OrderStatus.CANCELLED);
        d.recordPaymentOutcome(order.id(), transactionId, code);
        return new PaymentResult(cancelled, code);
    }

    private Order markCancelled(Dao d, Order order, OrderStatus next) throws SQLException {
        d.updateStatus(order.id(), next);
        Order cancelled = withStatus(order, next);
        notifier.notify(d, cancelled, NotificationType.ORDER_CANCELLED);
        return cancelled;
    }

    private static PaymentResult result(Order order, String outcome) {
        return OK.equals(outcome) ? new PaymentResult(order, null) : new PaymentResult(order, outcome);
    }

    private static Order lock(Dao d, String id) throws SQLException {
        return d.order(id, true).orElseThrow(() -> ApiException.notFound("ORDER_NOT_FOUND"));
    }

    private static Order withStatus(Order o, OrderStatus status) {
        return new Order(o.id(), o.cpf(), o.email(), status, o.createdAt(), o.summary(), o.items(),
                o.trackingCode());
    }

    private static Instant parseInstant(JsonNode node) {
        String text = CheckoutService.text(node);
        if (text == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
