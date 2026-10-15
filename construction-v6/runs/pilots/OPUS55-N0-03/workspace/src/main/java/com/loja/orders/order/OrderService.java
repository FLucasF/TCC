package com.loja.orders.order;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.loja.orders.catalog.InventoryService;
import com.loja.orders.checkout.CheckoutCalculator;
import com.loja.orders.checkout.CheckoutRequest;
import com.loja.orders.checkout.CheckoutSummary;
import com.loja.orders.common.ApiException;
import com.loja.orders.common.ErrorCode;
import com.loja.orders.customer.CustomerRepository;

@Service
public class OrderService {

    /** Pagamento que chega depois disso do registro do pedido não é aceito. */
    private static final Duration PAYMENT_DEADLINE = Duration.ofHours(48);

    private final CheckoutCalculator calculator;
    private final OrderRepository orders;
    private final CustomerRepository customers;
    private final InventoryService inventory;
    private final List<OrderLifecycleListener> listeners;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public OrderService(CheckoutCalculator calculator, OrderRepository orders, CustomerRepository customers,
            InventoryService inventory, List<OrderLifecycleListener> listeners, TransactionTemplate transaction,
            Clock clock) {
        this.calculator = calculator;
        this.orders = orders;
        this.customers = customers;
        this.inventory = inventory;
        this.listeners = listeners;
        this.transaction = transaction;
        this.clock = clock;
    }

    public Order place(CheckoutRequest request) {
        // O cálculo consulta a transportadora: fica fora da transação para não segurar o banco.
        CheckoutSummary summary = calculator.calculate(request);
        Order order = new Order(
                UUID.randomUUID().toString(),
                request.cpf(),
                request.email(),
                OrderStatus.AWAITING_PAYMENT,
                Instant.now(clock).truncatedTo(ChronoUnit.SECONDS),
                summary,
                request.items(),
                null);
        transaction.executeWithoutResult(status -> {
            customers.createIfAbsent(order.cpf());
            orders.insert(order);
        });
        return order;
    }

    @Transactional(readOnly = true)
    public Order get(String id) {
        return orders.findById(id).orElseThrow(OrderService::notFound);
    }

    @Transactional(noRollbackFor = PaymentRejectedException.class)
    public Order registerPayment(String id, String transactionId, String paidAtText) {
        Order order = orders.lockById(id).orElseThrow(OrderService::notFound);
        Instant paidAt = parseInstant(paidAtText);
        if (transactionId == null || paidAt == null) {
            throw ApiException.unprocessable(ErrorCode.INVALID_ORDER);
        }
        var previousNotice = orders.findPaymentNotice(id, transactionId);
        if (previousNotice.isPresent()) {
            return repeat(order, previousNotice.get());
        }
        requireTransition(order, OrderStatus.PAID);

        if (Duration.between(order.createdAt(), paidAt).compareTo(PAYMENT_DEADLINE) > 0) {
            return reject(order, transactionId, PaymentNoticeOutcome.ORDER_EXPIRED);
        }
        if (!inventory.take(order.items())) {
            return reject(order, transactionId, PaymentNoticeOutcome.INSUFFICIENT_STOCK);
        }
        orders.markPaid(id, transactionId, paidAt);
        orders.savePaymentNotice(id, transactionId, PaymentNoticeOutcome.ACCEPTED);
        Order paid = reload(id);
        listeners.forEach(listener -> listener.paymentApproved(paid));
        return paid;
    }

    @Transactional
    public Order ship(String id, String trackingCode) {
        Order order = orders.lockById(id).orElseThrow(OrderService::notFound);
        if (trackingCode == null) {
            throw ApiException.unprocessable(ErrorCode.INVALID_ORDER);
        }
        requireTransition(order, OrderStatus.SHIPPED);
        orders.markShipped(id, trackingCode);
        Order shipped = reload(id);
        listeners.forEach(listener -> listener.shipped(shipped));
        return shipped;
    }

    @Transactional
    public Order deliver(String id) {
        Order order = orders.lockById(id).orElseThrow(OrderService::notFound);
        requireTransition(order, OrderStatus.DELIVERED);
        orders.updateStatus(id, OrderStatus.DELIVERED);
        return reload(id);
    }

    @Transactional
    public Order cancel(String id) {
        Order order = orders.lockById(id).orElseThrow(OrderService::notFound);
        requireTransition(order, OrderStatus.CANCELLED);
        return cancelLocked(order);
    }

    private Order repeat(Order order, PaymentNoticeOutcome outcome) {
        return switch (outcome) {
            case ACCEPTED -> order;
            case ORDER_EXPIRED -> throw new PaymentRejectedException(ErrorCode.ORDER_EXPIRED);
            case INSUFFICIENT_STOCK -> throw new PaymentRejectedException(ErrorCode.INSUFFICIENT_STOCK);
        };
    }

    private Order reject(Order order, String transactionId, PaymentNoticeOutcome outcome) {
        orders.savePaymentNotice(order.id(), transactionId, outcome);
        cancelLocked(order);
        return repeat(order, outcome);
    }

    private Order cancelLocked(Order order) {
        orders.updateStatus(order.id(), OrderStatus.CANCELLED);
        Order cancelled = reload(order.id());
        listeners.forEach(listener -> listener.cancelled(cancelled, order.status()));
        return cancelled;
    }

    private Order reload(String id) {
        return orders.findById(id).orElseThrow(OrderService::notFound);
    }

    private static void requireTransition(Order order, OrderStatus next) {
        if (!order.status().canMoveTo(next)) {
            throw ApiException.conflict(ErrorCode.OPERATION_NOT_ALLOWED);
        }
    }

    private static Instant parseInstant(String text) {
        if (text == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(text).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static ApiException notFound() {
        return ApiException.notFound(ErrorCode.ORDER_NOT_FOUND);
    }
}
