package com.loja.service;

import com.loja.domain.*;
import com.loja.dto.OrderRequest;
import com.loja.dto.PaymentRequest;
import com.loja.exception.ErrorCode;
import com.loja.repository.CustomerRepository;
import com.loja.repository.NotificationRepository;
import com.loja.repository.OrderRepository;
import com.loja.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final NotificationRepository notificationRepository;
    private final CheckoutService checkoutService;

    private final Map<String, PaymentProcessingResult> processedPayments = new HashMap<>();

    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            NotificationRepository notificationRepository,
            CheckoutService checkoutService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.notificationRepository = notificationRepository;
        this.checkoutService = checkoutService;
    }

    @Transactional
    public Order createOrder(OrderRequest request) {
        OrderSummary summary = checkoutService.calculateSummary(request);

        List<OrderItem> items = request.items().stream()
            .map(i -> new OrderItem(i.sku(), i.quantity()))
            .toList();

        Order order = new Order(request.customer().cpf(), items, summary);

        customerRepository.findById(request.customer().cpf())
            .orElseGet(() -> customerRepository.save(new Customer(request.customer().cpf())));

        return orderRepository.save(order);
    }

    @Transactional
    public Order processPayment(Long orderId, PaymentRequest request) {
        if (request.transactionId() == null || request.paidAt() == null) {
            throw ErrorCode.INVALID_ORDER.asException();
        }

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> ErrorCode.ORDER_NOT_FOUND.asException());

        String paymentKey = orderId + ":" + request.transactionId();

        synchronized (processedPayments) {
            if (processedPayments.containsKey(paymentKey)) {
                PaymentProcessingResult cached = processedPayments.get(paymentKey);
                if (cached.success()) {
                    return orderRepository.findById(orderId).orElseThrow();
                } else {
                    throw cached.error().asException();
                }
            }
        }

        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            if (order.getPaymentTransactionId() != null &&
                order.getPaymentTransactionId().equals(request.transactionId())) {
                synchronized (processedPayments) {
                    processedPayments.put(paymentKey, new PaymentProcessingResult(true, null));
                }
                return order;
            }
            synchronized (processedPayments) {
                processedPayments.put(paymentKey, new PaymentProcessingResult(false, ErrorCode.OPERATION_NOT_ALLOWED));
            }
            throw ErrorCode.OPERATION_NOT_ALLOWED.asException();
        }

        Duration elapsed = Duration.between(order.getCreatedAt(), request.paidAt());
        if (elapsed.toHours() > 48) {
            order.cancel();
            orderRepository.save(order);
            notificationRepository.save(new Notification(
                order.getCustomerCpf(), NotificationType.ORDER_CANCELLED, orderId
            ));
            synchronized (processedPayments) {
                processedPayments.put(paymentKey, new PaymentProcessingResult(false, ErrorCode.ORDER_EXPIRED));
            }
            throw ErrorCode.ORDER_EXPIRED.asException();
        }

        try {
            decreaseStock(order);
        } catch (IllegalStateException e) {
            order.cancel();
            orderRepository.save(order);
            notificationRepository.save(new Notification(
                order.getCustomerCpf(), NotificationType.ORDER_CANCELLED, orderId
            ));
            synchronized (processedPayments) {
                processedPayments.put(paymentKey, new PaymentProcessingResult(false, ErrorCode.INSUFFICIENT_STOCK));
            }
            throw ErrorCode.INSUFFICIENT_STOCK.asException();
        }

        order.markAsPaid(request.transactionId());
        orderRepository.save(order);

        Customer customer = customerRepository.findByCpfForUpdate(order.getCustomerCpf())
            .orElseThrow(() -> ErrorCode.CUSTOMER_NOT_FOUND.asException());
        customer.addCredit(order.getSummary().nextPurchaseCredit());
        customerRepository.save(customer);

        notificationRepository.save(new Notification(
            order.getCustomerCpf(), NotificationType.PAYMENT_APPROVED, orderId
        ));

        synchronized (processedPayments) {
            processedPayments.put(paymentKey, new PaymentProcessingResult(true, null));
        }

        return order;
    }

    @Transactional
    public Order shipOrder(Long orderId, String trackingCode) {
        if (trackingCode == null || trackingCode.isBlank()) {
            throw ErrorCode.INVALID_ORDER.asException();
        }

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> ErrorCode.ORDER_NOT_FOUND.asException());

        if (order.getStatus() != OrderStatus.PAID) {
            throw ErrorCode.OPERATION_NOT_ALLOWED.asException();
        }

        order.ship(trackingCode);
        orderRepository.save(order);

        notificationRepository.save(new Notification(
            order.getCustomerCpf(), NotificationType.ORDER_SHIPPED, orderId
        ));

        return order;
    }

    @Transactional
    public Order deliverOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> ErrorCode.ORDER_NOT_FOUND.asException());

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw ErrorCode.OPERATION_NOT_ALLOWED.asException();
        }

        order.deliver();
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> ErrorCode.ORDER_NOT_FOUND.asException());

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw ErrorCode.OPERATION_NOT_ALLOWED.asException();
        }

        boolean wasPaid = order.isPaid();

        order.cancel();
        orderRepository.save(order);

        if (wasPaid) {
            increaseStock(order);

            Customer customer = customerRepository.findByCpfForUpdate(order.getCustomerCpf())
                .orElseThrow(() -> ErrorCode.CUSTOMER_NOT_FOUND.asException());
            customer.removeCredit(order.getSummary().nextPurchaseCredit());
            customerRepository.save(customer);
        }

        notificationRepository.save(new Notification(
            order.getCustomerCpf(), NotificationType.ORDER_CANCELLED, orderId
        ));

        return order;
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> ErrorCode.ORDER_NOT_FOUND.asException());
    }

    private void decreaseStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findBySkuForUpdate(item.getSku())
                .orElseThrow(() -> ErrorCode.UNKNOWN_PRODUCT.asException());
            product.decreaseStock(item.getQuantity());
            productRepository.save(product);
        }
    }

    private void increaseStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findBySkuForUpdate(item.getSku())
                .orElseThrow(() -> ErrorCode.UNKNOWN_PRODUCT.asException());
            product.increaseStock(item.getQuantity());
            productRepository.save(product);
        }
    }

    private record PaymentProcessingResult(boolean success, ErrorCode error) {}
}
