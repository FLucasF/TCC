package com.loja.service;

import com.loja.model.*;
import com.loja.repository.*;
import com.loja.strategy.*;
import com.loja.util.Money;
import java.time.Instant;
import java.util.*;

public class OrderService {
    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;
    private final CustomerRepository customerRepo;
    private final NotificationRepository notificationRepo;
    private final CheckoutService checkoutService;
    private final Map<String, String> paymentTracking = new HashMap<>();

    public OrderService(ProductRepository productRepo, OrderRepository orderRepo,
                       CustomerRepository customerRepo, NotificationRepository notificationRepo) {
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
        this.customerRepo = customerRepo;
        this.notificationRepo = notificationRepo;
        this.checkoutService = new CheckoutService(productRepo, this);
    }

    public Order createOrder(CheckoutRequest request) throws CheckoutException {
        OrderSummary summary = checkoutService.calculateSummary(request);

        String orderId = generateOrderId();
        Order order = new Order(
            orderId,
            request.customer.cpf,
            request.customer.email,
            OrderStatus.AWAITING_PAYMENT,
            Instant.now(),
            summary,
            new ArrayList<>(request.items)
        );

        orderRepo.create(order);

        Customer customer = customerRepo.findByCpf(request.customer.cpf);
        if (customer == null) {
            customerRepo.createOrUpdate(request.customer.cpf, 0.0);
        }

        return order;
    }

    public Order processPayment(String orderId, String transactionId, String paidAtStr) throws OrderException {
        Order order = orderRepo.findById(orderId);
        if (order == null) {
            throw new OrderException("ORDER_NOT_FOUND", 404);
        }

        if (transactionId == null || paidAtStr == null) {
            throw new OrderException("INVALID_ORDER", 422);
        }

        if (order.status != OrderStatus.AWAITING_PAYMENT) {
            throw new OrderException("OPERATION_NOT_ALLOWED", 409);
        }

        synchronized (paymentTracking) {
            String key = orderId + ":" + transactionId;
            if (paymentTracking.containsKey(key)) {
                return order;
            }
            paymentTracking.put(key, "processed");
        }

        Instant paidAt = Instant.parse(paidAtStr);
        Instant expiryTime = order.createdAt.plusSeconds(48 * 3600);

        if (paidAt.isAfter(expiryTime)) {
            orderRepo.updateStatus(orderId, OrderStatus.CANCELLED);
            Notification notification = new Notification(
                generateNotificationId(),
                NotificationType.ORDER_CANCELLED,
                orderId,
                order.cpf,
                NotificationStatus.NEW
            );
            notificationRepo.create(notification);
            throw new OrderException("ORDER_EXPIRED", 409);
        }

        validateStockAvailability(order);

        orderRepo.updateStatusAndPaidAt(orderId, OrderStatus.PAID, paidAt);

        deductStock(order);

        addCustomerCredit(order);

        Notification notification = new Notification(
            generateNotificationId(),
            NotificationType.PAYMENT_APPROVED,
            orderId,
            order.cpf,
            NotificationStatus.NEW
        );
        notificationRepo.create(notification);

        order.status = OrderStatus.PAID;
        order.paidAt = paidAt;
        return order;
    }

    public Order shipOrder(String orderId, String trackingCode) throws OrderException {
        Order order = orderRepo.findById(orderId);
        if (order == null) {
            throw new OrderException("ORDER_NOT_FOUND", 404);
        }

        if (trackingCode == null) {
            throw new OrderException("INVALID_ORDER", 422);
        }

        if (order.status != OrderStatus.PAID) {
            throw new OrderException("OPERATION_NOT_ALLOWED", 409);
        }

        orderRepo.updateTrackingCode(orderId, trackingCode);
        orderRepo.updateStatus(orderId, OrderStatus.SHIPPED);

        order.trackingCode = trackingCode;
        order.status = OrderStatus.SHIPPED;

        Notification notification = new Notification(
            generateNotificationId(),
            NotificationType.ORDER_SHIPPED,
            orderId,
            order.cpf,
            NotificationStatus.NEW
        );
        notificationRepo.create(notification);

        return order;
    }

    public Order deliverOrder(String orderId) throws OrderException {
        Order order = orderRepo.findById(orderId);
        if (order == null) {
            throw new OrderException("ORDER_NOT_FOUND", 404);
        }

        if (order.status != OrderStatus.SHIPPED) {
            throw new OrderException("OPERATION_NOT_ALLOWED", 409);
        }

        orderRepo.updateStatus(orderId, OrderStatus.DELIVERED);
        order.status = OrderStatus.DELIVERED;

        return order;
    }

    public Order cancelOrder(String orderId) throws OrderException {
        Order order = orderRepo.findById(orderId);
        if (order == null) {
            throw new OrderException("ORDER_NOT_FOUND", 404);
        }

        if (order.status == OrderStatus.DELIVERED || order.status == OrderStatus.CANCELLED) {
            throw new OrderException("OPERATION_NOT_ALLOWED", 409);
        }

        if (order.status == OrderStatus.SHIPPED) {
            throw new OrderException("OPERATION_NOT_ALLOWED", 409);
        }

        boolean wasPaid = order.status == OrderStatus.PAID;

        orderRepo.updateStatus(orderId, OrderStatus.CANCELLED);

        if (wasPaid) {
            restoreStock(order);
            subtractCustomerCredit(order);
        }

        Notification notification = new Notification(
            generateNotificationId(),
            NotificationType.ORDER_CANCELLED,
            orderId,
            order.cpf,
            NotificationStatus.NEW
        );
        notificationRepo.create(notification);

        order.status = OrderStatus.CANCELLED;
        return order;
    }

    private void validateStockAvailability(Order order) throws OrderException {
        for (OrderItem item : order.items) {
            Product product = productRepo.findBySku(item.sku);
            if (product == null || product.stock < item.quantity) {
                orderRepo.updateStatus(order.id, OrderStatus.CANCELLED);
                Notification notification = new Notification(
                    generateNotificationId(),
                    NotificationType.ORDER_CANCELLED,
                    order.id,
                    order.cpf,
                    NotificationStatus.NEW
                );
                notificationRepo.create(notification);
                throw new OrderException("INSUFFICIENT_STOCK", 409);
            }
        }
    }

    private void deductStock(Order order) {
        for (OrderItem item : order.items) {
            productRepo.updateStock(item.sku, item.quantity);
        }
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.items) {
            productRepo.restoreStock(item.sku, item.quantity);
        }
    }

    private void addCustomerCredit(Order order) {
        customerRepo.addCredit(order.cpf, order.summary.nextPurchaseCredit);
    }

    private void subtractCustomerCredit(Order order) {
        customerRepo.subtractCredit(order.cpf, order.summary.nextPurchaseCredit);
    }

    private String generateOrderId() {
        return UUID.randomUUID().toString();
    }

    private String generateNotificationId() {
        return UUID.randomUUID().toString();
    }

    public Order findOrder(String orderId) {
        return orderRepo.findById(orderId);
    }

    public OrderRepository getOrderRepo() {
        return orderRepo;
    }
}
