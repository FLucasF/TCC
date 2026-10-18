package br.com.loja.pedidos.order;

import br.com.loja.pedidos.catalog.ProductRepository;
import br.com.loja.pedidos.checkout.CheckoutRequest;
import br.com.loja.pedidos.checkout.CheckoutService;
import br.com.loja.pedidos.checkout.OrderSummary;
import br.com.loja.pedidos.common.ApiException;
import br.com.loja.pedidos.customer.CustomerRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderService {

    private static final Duration PAYMENT_WINDOW = Duration.ofHours(48);
    private static final String PAYMENT_OK = "OK";

    private final CheckoutService checkout;
    private final OrderRepository orders;
    private final ProductRepository products;
    private final CustomerRepository customers;
    private final List<OrderLifecycleListener> listeners;
    private final TransactionTemplate tx;
    private final Clock clock;

    public OrderService(CheckoutService checkout, OrderRepository orders, ProductRepository products,
                        CustomerRepository customers, List<OrderLifecycleListener> listeners,
                        TransactionTemplate tx, Clock clock) {
        this.checkout = checkout;
        this.orders = orders;
        this.products = products;
        this.customers = customers;
        this.listeners = listeners;
        this.tx = tx;
        this.clock = clock;
    }

    public OrderSummary summarize(CheckoutRequest request) {
        return checkout.summarize(request);
    }

    public Order register(CheckoutRequest request) {
        // computed before opening the transaction: it may call the carrier
        OrderSummary summary = checkout.summarize(request);
        List<Order.Item> items = request.items().stream()
                .map(i -> new Order.Item(i.sku(), i.quantity())).toList();
        Order order = new Order(UUID.randomUUID().toString(), OrderStatus.AWAITING_PAYMENT,
                clock.instant().truncatedTo(ChronoUnit.SECONDS), summary, items, null);
        customers.createIfAbsent(request.cpf(), request.email());
        return inTransaction(() -> {
            orders.insert(request.cpf(), request.email(), order);
            return order;
        });
    }

    public Order get(String id) {
        return orders.find(id).orElseThrow(OrderService::notFound).order();
    }

    public Order confirmPayment(String id, String transactionId, String paidAt) {
        PaymentResult result = inTransaction(() -> doConfirmPayment(id, transactionId, paidAt));
        if (result.error() != null) {
            throw result.error();
        }
        return result.order();
    }

    public Order ship(String id, String trackingCode) {
        return inTransaction(() -> {
            StoredOrder stored = lock(id);
            if (trackingCode == null || trackingCode.isBlank()) {
                throw ApiException.unprocessable("INVALID_ORDER");
            }
            requireTransition(stored, OrderStatus.SHIPPED);
            orders.updateShipment(id, trackingCode);
            StoredOrder shipped = reload(id);
            listeners.forEach(l -> l.onShipped(shipped));
            return shipped.order();
        });
    }

    public Order deliver(String id) {
        return inTransaction(() -> {
            requireTransition(lock(id), OrderStatus.DELIVERED);
            orders.updateStatus(id, OrderStatus.DELIVERED);
            return reload(id).order();
        });
    }

    public Order cancel(String id) {
        return inTransaction(() -> {
            StoredOrder stored = lock(id);
            requireTransition(stored, OrderStatus.CANCELLED);
            cancelLocked(stored);
            return reload(id).order();
        });
    }

    private PaymentResult doConfirmPayment(String id, String transactionId, String paidAtText) {
        StoredOrder stored = lock(id);
        Instant paidAt = parseInstant(paidAtText);
        if (transactionId == null || transactionId.isBlank() || paidAt == null) {
            throw ApiException.unprocessable("INVALID_ORDER");
        }
        String previous = orders.findPaymentOutcome(id, transactionId).orElse(null);
        if (previous != null) {
            return PAYMENT_OK.equals(previous)
                    ? new PaymentResult(stored.order(), null)
                    : new PaymentResult(null, ApiException.conflict(previous));
        }
        requireTransition(stored, OrderStatus.PAID);

        Order order = stored.order();
        if (paidAt.isAfter(order.createdAt().plus(PAYMENT_WINDOW))) {
            return refuse(stored, transactionId, "ORDER_EXPIRED");
        }
        // lock in a fixed order so that concurrent payments cannot deadlock
        List<Order.Item> items = order.items().stream().sorted(Comparator.comparing(Order.Item::sku)).toList();
        boolean enough = true;
        for (Order.Item item : items) {
            enough &= products.lockStock(item.sku()) >= item.quantity();
        }
        if (!enough) {
            return refuse(stored, transactionId, "INSUFFICIENT_STOCK");
        }
        items.forEach(i -> products.decrementStock(i.sku(), i.quantity()));
        orders.updateStatus(id, OrderStatus.PAID);
        orders.recordPaymentOutcome(id, transactionId, PAYMENT_OK);
        StoredOrder paid = reload(id);
        listeners.forEach(l -> l.onPaid(paid));
        return new PaymentResult(paid.order(), null);
    }

    private PaymentResult refuse(StoredOrder stored, String transactionId, String code) {
        orders.recordPaymentOutcome(stored.order().id(), transactionId, code);
        cancelLocked(stored);
        return new PaymentResult(null, ApiException.conflict(code));
    }

    /** Cancels an order whose row is already locked, giving back whatever the payment had taken. */
    private void cancelLocked(StoredOrder stored) {
        boolean wasPaid = stored.order().status() == OrderStatus.PAID;
        if (wasPaid) {
            stored.order().items().stream().sorted(Comparator.comparing(Order.Item::sku))
                    .forEach(i -> products.incrementStock(i.sku(), i.quantity()));
        }
        orders.updateStatus(stored.order().id(), OrderStatus.CANCELLED);
        StoredOrder cancelled = reload(stored.order().id());
        listeners.forEach(l -> l.onCancelled(cancelled, wasPaid));
    }

    private StoredOrder lock(String id) {
        return orders.findForUpdate(id).orElseThrow(OrderService::notFound);
    }

    private StoredOrder reload(String id) {
        return orders.find(id).orElseThrow(OrderService::notFound);
    }

    private static void requireTransition(StoredOrder stored, OrderStatus next) {
        if (!stored.order().status().canTransitionTo(next)) {
            throw ApiException.conflict("OPERATION_NOT_ALLOWED");
        }
    }

    private static ApiException notFound() {
        return ApiException.notFound("ORDER_NOT_FOUND");
    }

    private static Instant parseInstant(String text) {
        if (text == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text).toInstant();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private <T> T inTransaction(Supplier<T> work) {
        return tx.execute(status -> work.get());
    }

    private record PaymentResult(Order order, ApiException error) {
    }
}
