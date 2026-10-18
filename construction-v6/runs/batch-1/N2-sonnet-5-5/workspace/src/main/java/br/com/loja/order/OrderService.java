package br.com.loja.order;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.loja.catalog.ProductRepository;
import br.com.loja.checkout.CheckoutRequest;
import br.com.loja.checkout.CheckoutService;
import br.com.loja.checkout.OrderSummary;
import br.com.loja.common.ApiException;
import br.com.loja.common.ErrorCode;
import br.com.loja.customer.CustomerRepository;

@Service
public class OrderService {

    private static final String OK = "OK";
    private static final Duration PAYMENT_WINDOW = Duration.ofHours(48);

    private final CheckoutService checkout;
    private final OrderRepository orders;
    private final ProductRepository products;
    private final CustomerRepository customers;
    private final List<OrderEventListener> listeners;
    private final Clock clock;
    private final TransactionTemplate tx;

    OrderService(CheckoutService checkout, OrderRepository orders, ProductRepository products,
                 CustomerRepository customers, List<OrderEventListener> listeners, Clock clock,
                 PlatformTransactionManager transactionManager) {
        this.checkout = checkout;
        this.orders = orders;
        this.products = products;
        this.customers = customers;
        this.listeners = listeners;
        this.clock = clock;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public Order create(CheckoutRequest request) {
        OrderSummary summary = checkout.summarize(request);
        List<OrderItem> items = request.items().stream()
                .map(i -> new OrderItem(i.sku(), i.quantity())).toList();
        Order order = new Order(UUID.randomUUID().toString(), request.customer().cpf(),
                OrderStatus.AWAITING_PAYMENT, Instant.now(clock).truncatedTo(ChronoUnit.SECONDS), summary, items,
                null, null, null);
        try {
            tx.executeWithoutResult(s -> customers.createIfAbsent(request.customer().cpf(), request.customer().email()));
        } catch (DuplicateKeyException e) {
            // outra máquina criou o mesmo cliente agora há pouco: está tudo certo
        }
        tx.executeWithoutResult(s -> orders.insert(order));
        return order;
    }

    public Order get(String id) {
        return orders.findById(id).orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND));
    }

    private record PaymentResult(Order order, ErrorCode error) {
    }

    public Order confirmPayment(String id, String transactionId, String paidAtText) {
        PaymentResult result = tx.execute(s -> processPayment(id, transactionId, paidAtText));
        if (result.error() != null) {
            throw new ApiException(result.error(), HttpStatus.CONFLICT);
        }
        return result.order();
    }

    private PaymentResult processPayment(String id, String transactionId, String paidAtText) {
        Order order = lock(id);
        Instant paidAt = parseInstant(paidAtText);
        if (transactionId == null || transactionId.isBlank() || paidAt == null) {
            throw new ApiException(ErrorCode.INVALID_ORDER);
        }
        if (transactionId.equals(order.paymentTransactionId())) {
            return outcomeOf(order, order.paymentOutcome());
        }
        requireMove(order, OrderStatus.PAID);

        if (paidAt.isAfter(order.createdAt().plus(PAYMENT_WINDOW))) {
            return failPayment(order, transactionId, ErrorCode.ORDER_EXPIRED);
        }
        if (!products.reserve(order.items())) {
            return failPayment(order, transactionId, ErrorCode.INSUFFICIENT_STOCK);
        }
        orders.recordPayment(id, transactionId, OK, OrderStatus.PAID);
        Order paid = get(id);
        listeners.forEach(l -> l.onPaid(paid));
        return new PaymentResult(paid, null);
    }

    private PaymentResult failPayment(Order order, String transactionId, ErrorCode error) {
        orders.recordPayment(order.id(), transactionId, error.name(), OrderStatus.CANCELLED);
        Order cancelled = get(order.id());
        listeners.forEach(l -> l.onCancelled(cancelled, false));
        return new PaymentResult(cancelled, error);
    }

    private static PaymentResult outcomeOf(Order order, String outcome) {
        return new PaymentResult(order, OK.equals(outcome) ? null : ErrorCode.valueOf(outcome));
    }

    public Order ship(String id, String trackingCode) {
        return tx.execute(s -> {
            Order order = lock(id);
            if (trackingCode == null || trackingCode.isBlank()) {
                throw new ApiException(ErrorCode.INVALID_ORDER);
            }
            requireMove(order, OrderStatus.SHIPPED);
            orders.recordShipment(id, trackingCode);
            Order shipped = get(id);
            listeners.forEach(l -> l.onShipped(shipped));
            return shipped;
        });
    }

    public Order deliver(String id) {
        return tx.execute(s -> {
            Order order = lock(id);
            requireMove(order, OrderStatus.DELIVERED);
            orders.updateStatus(id, OrderStatus.DELIVERED);
            return get(id);
        });
    }

    public Order cancel(String id) {
        return tx.execute(s -> {
            Order order = lock(id);
            requireMove(order, OrderStatus.CANCELLED);
            boolean wasPaid = order.status() == OrderStatus.PAID;
            if (wasPaid) {
                products.release(order.items());
            }
            orders.updateStatus(id, OrderStatus.CANCELLED);
            Order cancelled = get(id);
            listeners.forEach(l -> l.onCancelled(cancelled, wasPaid));
            return cancelled;
        });
    }

    private Order lock(String id) {
        return orders.findByIdForUpdate(id).orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND));
    }

    private static void requireMove(Order order, OrderStatus target) {
        if (!order.status().canMoveTo(target)) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED);
        }
    }

    private static Instant parseInstant(String text) {
        try {
            return text == null ? null : Instant.parse(text);
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }
}
