package br.com.loja.pedidos.order;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.loja.pedidos.catalog.StockService;
import br.com.loja.pedidos.checkout.CheckoutRequest;
import br.com.loja.pedidos.checkout.CheckoutService;
import br.com.loja.pedidos.common.DomainException;
import br.com.loja.pedidos.customer.CustomerRepository;

/**
 * Operações sobre pedidos. Cada uma trava o pedido, então operações simultâneas
 * (inclusive vindas das duas máquinas) acontecem uma de cada vez.
 * Recusas gravam seus efeitos (cancelamento, registro do aviso) e por isso não desfazem a transação.
 */
@Service
@Transactional(noRollbackFor = DomainException.class)
public class OrderService {

    private static final Duration PAYMENT_WINDOW = Duration.ofHours(48);

    private final CheckoutService checkout;
    private final OrderRepository orders;
    private final CustomerRepository customers;
    private final StockService stock;
    private final List<OrderEventListener> listeners;
    private final Clock clock;

    public OrderService(CheckoutService checkout, OrderRepository orders, CustomerRepository customers,
                        StockService stock, List<OrderEventListener> listeners, Clock clock) {
        this.checkout = checkout;
        this.orders = orders;
        this.customers = customers;
        this.stock = stock;
        this.listeners = listeners;
        this.clock = clock;
    }

    public Order register(CheckoutRequest request) {
        var summary = checkout.calculate(request);
        customers.createIfAbsent(request.customer().cpf());
        Order order = new Order(UUID.randomUUID().toString(), OrderStatus.AWAITING_PAYMENT,
                Instant.now(clock).truncatedTo(ChronoUnit.SECONDS), summary, request.orderItems(), null,
                request.customer().cpf(), request.customer().email());
        orders.insert(order);
        return order;
    }

    @Transactional(readOnly = true)
    public Order get(String id) {
        return orders.find(id).orElseThrow(OrderService::notFound);
    }

    public Order confirmPayment(String id, String transactionId, String paidAt) {
        Order order = lock(id);
        if (isBlank(transactionId) || isBlank(paidAt)) {
            throw DomainException.unprocessable("INVALID_ORDER");
        }
        Instant paidInstant = parseInstant(paidAt);

        var previous = orders.findPaymentNotice(id, transactionId);
        if (previous.isPresent()) {
            if (previous.get().isEmpty()) {
                return order;
            }
            throw DomainException.conflict(previous.get());
        }
        if (!order.status().acceptsPayment()) {
            throw DomainException.conflict("OPERATION_NOT_ALLOWED");
        }
        if (paidInstant.isAfter(order.createdAt().plus(PAYMENT_WINDOW))) {
            throw refuse(order, transactionId, "ORDER_EXPIRED");
        }
        try {
            stock.reserve(order.items());
        } catch (DomainException e) {
            throw refuse(order, transactionId, e.code());
        }
        orders.update(id, OrderStatus.PAID, order.trackingCode());
        Order paid = withStatus(order, OrderStatus.PAID, order.trackingCode());
        listeners.forEach(listener -> listener.paymentConfirmed(paid));
        orders.savePaymentNotice(id, transactionId, null);
        return paid;
    }

    public Order ship(String id, String trackingCode) {
        Order order = lock(id);
        if (isBlank(trackingCode)) {
            throw DomainException.unprocessable("INVALID_ORDER");
        }
        if (!order.status().canShip()) {
            throw DomainException.conflict("OPERATION_NOT_ALLOWED");
        }
        orders.update(id, OrderStatus.SHIPPED, trackingCode);
        Order shipped = withStatus(order, OrderStatus.SHIPPED, trackingCode);
        listeners.forEach(listener -> listener.shipped(shipped));
        return shipped;
    }

    public Order deliver(String id) {
        Order order = lock(id);
        if (!order.status().canDeliver()) {
            throw DomainException.conflict("OPERATION_NOT_ALLOWED");
        }
        orders.update(id, OrderStatus.DELIVERED, order.trackingCode());
        return withStatus(order, OrderStatus.DELIVERED, order.trackingCode());
    }

    public Order cancel(String id) {
        Order order = lock(id);
        if (!order.status().canCancel()) {
            throw DomainException.conflict("OPERATION_NOT_ALLOWED");
        }
        return cancelLocked(order);
    }

    private DomainException refuse(Order order, String transactionId, String code) {
        cancelLocked(order);
        orders.savePaymentNotice(order.id(), transactionId, code);
        return DomainException.conflict(code);
    }

    private Order cancelLocked(Order order) {
        boolean wasPaid = order.status().paymentReceived();
        orders.update(order.id(), OrderStatus.CANCELLED, order.trackingCode());
        Order cancelled = withStatus(order, OrderStatus.CANCELLED, order.trackingCode());
        if (wasPaid) {
            stock.release(order.items());
        }
        listeners.forEach(listener -> listener.cancelled(cancelled, wasPaid));
        return cancelled;
    }

    private Order lock(String id) {
        return orders.findForUpdate(id).orElseThrow(OrderService::notFound);
    }

    private static Order withStatus(Order order, OrderStatus status, String trackingCode) {
        return new Order(order.id(), status, order.createdAt(), order.summary(), order.items(), trackingCode,
                order.cpf(), order.email());
    }

    private static Instant parseInstant(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw DomainException.unprocessable("INVALID_ORDER");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static DomainException notFound() {
        return DomainException.notFound("ORDER_NOT_FOUND");
    }
}
