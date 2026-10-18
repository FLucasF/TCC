package com.loja.services;

import com.loja.domain.entities.*;
import com.loja.domain.events.PaymentConfirmedEvent;
import com.loja.domain.observers.PaymentConfirmedObserver;
import com.loja.domain.observers.StockObserver;
import com.loja.repositories.CustomerRepository;
import com.loja.repositories.NotificationRepository;
import com.loja.repositories.OrderRepository;
import com.loja.repositories.ProductRepository;
import com.loja.repositories.impl.OrderRepositoryImpl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OrderService {
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final NotificationRepository notificationRepository;
    private final ProductRepository productRepository;
    private final List<PaymentConfirmedObserver> paymentObservers;

    public OrderService(
            OrderRepository orderRepository,
            CustomerRepository customerRepository,
            NotificationRepository notificationRepository,
            ProductRepository productRepository,
            List<PaymentConfirmedObserver> paymentObservers) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.notificationRepository = notificationRepository;
        this.productRepository = productRepository;
        this.paymentObservers = paymentObservers;
    }

    public Order createOrder(String customerCpf, String customerEmail, List<OrderItem> items, OrderSummary summary) {
        if (!customerRepository.exists(customerCpf)) {
            Customer customer = new Customer(customerCpf);
            customerRepository.save(customer);
        }

        String orderId = UUID.randomUUID().toString();
        Order order = new Order(
                orderId,
                customerCpf,
                customerEmail,
                items,
                summary,
                OrderStatus.AWAITING_PAYMENT,
                Instant.now()
        );

        orderRepository.save(order);
        return order;
    }

    public Order confirmPayment(String orderId, String transactionId, Instant paidAt) throws PaymentException {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new PaymentException("Order not found");
        }

        if (orderRepository.hasPaymentTransaction(orderId, transactionId)) {
            return order;
        }

        if (!order.canBePaid()) {
            throw new PaymentException("Order cannot be paid in current status: " + order.getStatus());
        }

        if (order.isExpired(paidAt)) {
            order.cancel();
            orderRepository.update(order);
            ((OrderRepositoryImpl) orderRepository).savePaymentTransaction(orderId, transactionId);

            notifyCustomerCancellation(order);

            throw new OrderExpiredException("Order expired");
        }

        try {
            notifyPaymentConfirmed(order);

            order.confirmPayment(transactionId, paidAt);
            orderRepository.update(order);
            ((OrderRepositoryImpl) orderRepository).savePaymentTransaction(orderId, transactionId);

            return order;

        } catch (StockObserver.InsufficientStockException e) {
            order.cancel();
            orderRepository.update(order);
            ((OrderRepositoryImpl) orderRepository).savePaymentTransaction(orderId, transactionId);

            notifyCustomerCancellation(order);

            throw new InsufficientStockException("Insufficient stock");
        } catch (Exception e) {
            throw new PaymentException("Failed to confirm payment", e);
        }
    }

    public Order shipOrder(String orderId, String trackingCode) throws ShipmentException {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new ShipmentException("Order not found");
        }

        if (!order.canBeShipped()) {
            throw new ShipmentException("Order cannot be shipped in current status: " + order.getStatus());
        }

        order.ship(trackingCode);
        orderRepository.update(order);

        notifyCustomerShipment(order);

        return order;
    }

    public Order deliverOrder(String orderId) throws DeliveryException {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new DeliveryException("Order not found");
        }

        if (!order.canBeDelivered()) {
            throw new DeliveryException("Order cannot be delivered in current status: " + order.getStatus());
        }

        order.deliver();
        orderRepository.update(order);

        return order;
    }

    public Order cancelOrder(String orderId) throws CancellationException {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new CancellationException("Order not found");
        }

        if (!order.canBeCancelled()) {
            throw new CancellationException("Order cannot be cancelled in current status: " + order.getStatus());
        }

        boolean wasPaid = order.getStatus() == OrderStatus.PAID;

        order.cancel();
        orderRepository.update(order);

        if (wasPaid) {
            reversePaymentEffects(order);
        }

        notifyCustomerCancellation(order);

        return order;
    }

    private void notifyPaymentConfirmed(Order order) throws Exception {
        PaymentConfirmedEvent event = new PaymentConfirmedEvent(order);

        for (PaymentConfirmedObserver observer : paymentObservers) {
            observer.onPaymentConfirmed(event);
        }
    }

    private void reversePaymentEffects(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findBySku(item.getSku());
            product.increaseStock(item.getQuantity());
            productRepository.update(product);
        }

        Money creditAmount = order.getSummary().getNextPurchaseCredit();
        if (creditAmount.isPositive()) {
            Customer customer = customerRepository.findByCpf(order.getCustomerCpf());
            customer.subtractCredit(creditAmount);
            customerRepository.update(customer);
        }
    }

    private void notifyCustomerShipment(Order order) {
        Notification notification = new Notification(
                0,
                NotificationType.ORDER_SHIPPED,
                order.getId(),
                NotificationStatus.NEW
        );
        notificationRepository.save(order.getCustomerCpf(), notification);
    }

    private void notifyCustomerCancellation(Order order) {
        Notification notification = new Notification(
                0,
                NotificationType.ORDER_CANCELLED,
                order.getId(),
                NotificationStatus.NEW
        );
        notificationRepository.save(order.getCustomerCpf(), notification);
    }

    public static class PaymentException extends Exception {
        public PaymentException(String message) {
            super(message);
        }

        public PaymentException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class OrderExpiredException extends PaymentException {
        public OrderExpiredException(String message) {
            super(message);
        }
    }

    public static class InsufficientStockException extends PaymentException {
        public InsufficientStockException(String message) {
            super(message);
        }
    }

    public static class ShipmentException extends Exception {
        public ShipmentException(String message) {
            super(message);
        }
    }

    public static class DeliveryException extends Exception {
        public DeliveryException(String message) {
            super(message);
        }
    }

    public static class CancellationException extends Exception {
        public CancellationException(String message) {
            super(message);
        }
    }
}
