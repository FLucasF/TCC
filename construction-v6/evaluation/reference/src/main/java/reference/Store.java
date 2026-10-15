package reference;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import reference.Calculator.Context;
import reference.Catalog.Product;
import reference.Purchase.Item;
import reference.Result.Refusal;
import reference.Result.Summary;

/**
 * The reference for the whole store: the order lifecycle, the stock, the customers' credit and the notifications.
 * It is the "state calculator": the suite makes the SAME sequence of calls here and on the agent's service
 * and compares the answers at every step. Each public method is a route of the prompt's API.
 *
 * HOW TO READ. The state is four collections: the stock, the credit balances, the orders and the
 * notifications. The operations on an order check, in this order: the order exists (404), the required
 * fields came (422), the status allows it (409); and, on payment, the 48 hours and the stock (409,
 * cancelling the order).
 *
 * What does NOT fit here: concurrency. With two payments at the same time, either one may win; the suite
 * checks the rules that hold for any winner, not who won.
 */
public final class Store {

    public enum Status { AWAITING_PAYMENT, PAID, SHIPPED, DELIVERED, CANCELLED }

    public enum NotificationType { PAYMENT_APPROVED, ORDER_SHIPPED, ORDER_CANCELLED }

    public enum NotificationStatus { NEW, READ }

    /** A route's answer: the body with the success HTTP status, or the error code with the HTTP status. */
    public sealed interface Response<T> permits Success, Failure {}

    public record Success<T>(int http, T body) implements Response<T> {}

    public record Failure<T>(int http, String error) implements Response<T> {}

    /** A registered order. The summary and the items do not change after it is registered. */
    public static final class Order {
        public final String id;
        public final String cpf;
        public final Instant createdAt;
        public final Summary summary;
        public final List<Item> items;
        public Status status = Status.AWAITING_PAYMENT;
        public String trackingCode;                              // null until shipment
        /** The answer given to each transactionId: the same notice repeated returns the same answer. */
        private final Map<String, Response<Order>> paymentAnswers = new HashMap<>();

        Order(String id, String cpf, Instant createdAt, Summary summary, List<Item> items) {
            this.id = id;
            this.cpf = cpf;
            this.createdAt = createdAt;
            this.summary = summary;
            this.items = List.copyOf(items);
        }
    }

    public static final class Notification {
        public final String id;
        public final NotificationType type;
        public final String orderId;
        public final String cpf;
        public NotificationStatus status = NotificationStatus.NEW;

        Notification(String id, NotificationType type, String orderId, String cpf) {
            this.id = id;
            this.type = type;
            this.orderId = orderId;
            this.cpf = cpf;
        }
    }

    static final Duration PAYMENT_DEADLINE = Duration.ofHours(48);

    // ------------------------------------------------------------------ the state
    private final Map<String, Product> products = new LinkedHashMap<>();     // with the current stock
    private final Map<String, BigDecimal> creditBalances = new LinkedHashMap<>();  // one per customer (CPF)
    private final Map<String, Order> orders = new LinkedHashMap<>();
    private final List<Notification> notifications = new ArrayList<>();
    private final TransLog transLog;
    private int nextOrderId = 1;
    private int nextNotificationId = 1;

    public Store(Catalog catalog, TransLog transLog) {
        catalog.all().forEach(p -> products.put(p.sku(), p));
        this.transLog = transLog;
    }

    // ------------------------------------------------------------------ the routes

    /** POST /checkout/summary */
    public Response<Summary> checkoutSummary(Purchase purchase) {
        return switch (Calculator.calculate(purchase, calculatorContext())) {
            case Summary summary -> new Success<>(200, summary);
            case Refusal refusal -> new Failure<>(refusal.http(), refusal.error());
        };
    }

    /** POST /orders. The summary is the same as /checkout/summary's; the stock is only checked, not taken. */
    public Response<Order> createOrder(Purchase purchase, Instant now) {
        return switch (Calculator.calculate(purchase, calculatorContext())) {
            case Refusal refusal -> new Failure<>(refusal.http(), refusal.error());
            case Summary summary -> {
                String cpf = purchase.customer().cpf();
                creditBalances.putIfAbsent(cpf, money("0.00"));    // the customer now exists, with a zero balance
                Order order = new Order(String.valueOf(nextOrderId++), cpf, now, summary, purchase.items());
                orders.put(order.id, order);
                yield new Success<>(201, order);
            }
        };
    }

    /** GET /orders/{id}. An unpaid order keeps awaiting payment, even after the 48 hours. */
    public Response<Order> getOrder(String id) {
        Order order = orders.get(id);
        return order == null ? notFound("ORDER_NOT_FOUND") : new Success<>(200, order);
    }

    /** POST /orders/{id}/payment */
    public Response<Order> pay(String id, String transactionId, Instant paidAt) {
        Order order = orders.get(id);
        if (order == null) return notFound("ORDER_NOT_FOUND");
        if (isBlank(transactionId) || paidAt == null) return failure(422, "INVALID_ORDER");

        // the same notice repeated: the same answer as the first time, doing nothing new
        Response<Order> firstAnswer = order.paymentAnswers.get(transactionId);
        if (firstAnswer instanceof Success<Order>) return new Success<>(200, order);   // the order as it is now
        if (firstAnswer != null) return firstAnswer;                                   // the same refusal

        // a notice with another transactionId, on an order that no longer awaits payment
        if (order.status != Status.AWAITING_PAYMENT) return failure(409, "OPERATION_NOT_ALLOWED");

        Response<Order> answer;
        if (paidAt.isAfter(order.createdAt.plus(PAYMENT_DEADLINE))) {    // at exactly 48 hours, still accepted
            cancel(order);
            answer = failure(409, "ORDER_EXPIRED");
        } else if (!hasStockFor(order)) {
            cancel(order);
            answer = failure(409, "INSUFFICIENT_STOCK");
        } else {
            order.items.forEach(item -> changeStock(item, -item.quantity()));
            creditBalances.merge(order.cpf, order.summary.nextPurchaseCredit(), BigDecimal::add);  // the summary's credit
            order.status = Status.PAID;
            notify(NotificationType.PAYMENT_APPROVED, order);
            answer = new Success<>(200, order);
        }
        order.paymentAnswers.put(transactionId, answer);
        return answer;
    }

    /** POST /orders/{id}/shipment */
    public Response<Order> ship(String id, String trackingCode) {
        Order order = orders.get(id);
        if (order == null) return notFound("ORDER_NOT_FOUND");
        if (isBlank(trackingCode)) return failure(422, "INVALID_ORDER");
        if (order.status != Status.PAID) return failure(409, "OPERATION_NOT_ALLOWED");
        order.status = Status.SHIPPED;
        order.trackingCode = trackingCode;
        notify(NotificationType.ORDER_SHIPPED, order);
        return new Success<>(200, order);
    }

    /** POST /orders/{id}/delivery */
    public Response<Order> deliver(String id) {
        Order order = orders.get(id);
        if (order == null) return notFound("ORDER_NOT_FOUND");
        if (order.status != Status.SHIPPED) return failure(409, "OPERATION_NOT_ALLOWED");
        order.status = Status.DELIVERED;                       // delivery creates no notification
        return new Success<>(200, order);
    }

    /** POST /orders/{id}/cancellation. Only while the order has not been shipped. */
    public Response<Order> cancel(String id) {
        Order order = orders.get(id);
        if (order == null) return notFound("ORDER_NOT_FOUND");
        if (order.status != Status.AWAITING_PAYMENT && order.status != Status.PAID) return failure(409, "OPERATION_NOT_ALLOWED");
        cancel(order);
        return new Success<>(200, order);
    }

    /** GET /products/{sku}, with the current stock. */
    public Response<Product> getProduct(String sku) {
        Product product = products.get(sku);
        return product == null ? notFound("PRODUCT_NOT_FOUND") : new Success<>(200, product);
    }

    /** GET /customers/{cpf}: the credit balance. */
    public Response<BigDecimal> getCreditBalance(String cpf) {
        BigDecimal balance = creditBalances.get(cpf);
        return balance == null ? notFound("CUSTOMER_NOT_FOUND") : new Success<>(200, balance);
    }

    /** GET /customers/{cpf}/notifications, oldest first. A CPF with no order: 404. */
    public Response<List<Notification>> getNotifications(String cpf) {
        if (!creditBalances.containsKey(cpf)) return notFound("CUSTOMER_NOT_FOUND");
        return new Success<>(200, notifications.stream().filter(n -> n.cpf.equals(cpf)).toList());
    }

    /** POST /notifications/{id}/read. Reading an already read notification again changes nothing. */
    public Response<Notification> markAsRead(String id) {
        for (Notification n : notifications) {
            if (n.id.equals(id)) {
                n.status = NotificationStatus.READ;
                return new Success<>(200, n);
            }
        }
        return notFound("NOTIFICATION_NOT_FOUND");
    }

    // ------------------------------------------------------------------ the internal rules

    /** The calculator's context with the state of NOW: the current stock and the history of paid purchases. */
    private Context calculatorContext() {
        return new Context(new Catalog(List.copyOf(products.values())), this::hasPaidOrder, transLog);
    }

    /** "First purchase" = no paid, shipped or delivered order (a cancelled order does not count). */
    boolean hasPaidOrder(String cpf) {
        return orders.values().stream().anyMatch(o -> o.cpf.equals(cpf)
                && (o.status == Status.PAID || o.status == Status.SHIPPED || o.status == Status.DELIVERED));
    }

    /** All or nothing: the payment only goes through if there is stock for every item. */
    private boolean hasStockFor(Order order) {
        return order.items.stream().allMatch(item -> products.get(item.sku()).stock() >= item.quantity());
    }

    /** Every cancellation notifies the customer; cancelling a paid order also returns the stock and removes the credit. */
    private void cancel(Order order) {
        if (order.status == Status.PAID) {
            order.items.forEach(item -> changeStock(item, item.quantity()));
            creditBalances.merge(order.cpf, order.summary.nextPurchaseCredit().negate(), BigDecimal::add);
        }
        order.status = Status.CANCELLED;
        notify(NotificationType.ORDER_CANCELLED, order);
    }

    private void changeStock(Item item, int delta) {
        Product product = products.get(item.sku());
        products.put(item.sku(), product.withStock(product.stock() + delta));
    }

    private void notify(NotificationType type, Order order) {
        notifications.add(new Notification(String.valueOf(nextNotificationId++), type, order.id, order.cpf));
    }

    private static <T> Failure<T> notFound(String error) {
        return new Failure<>(404, error);
    }

    private static <T> Failure<T> failure(int http, String error) {
        return new Failure<>(http, error);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
