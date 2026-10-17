package com.loja.orderservice.service;

import com.loja.orderservice.dto.OrderItemDTO;
import com.loja.orderservice.dto.OrderRequestDTO;
import com.loja.orderservice.entity.*;
import com.loja.orderservice.exception.OrderException;
import com.loja.orderservice.repository.CustomerRepository;
import com.loja.orderservice.repository.CustomerNotificationRepository;
import com.loja.orderservice.repository.OrderRepository;
import com.loja.orderservice.repository.ProductRepository;
import com.loja.orderservice.util.PricingUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final CustomerNotificationRepository notificationRepository;
    private final PricingService pricingService;
    private final ValidationService validationService;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository,
                        CustomerRepository customerRepository, CustomerNotificationRepository notificationRepository,
                        PricingService pricingService, ValidationService validationService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.notificationRepository = notificationRepository;
        this.pricingService = pricingService;
        this.validationService = validationService;
    }

    public OrderSummary calculateSummary(OrderRequestDTO request) {
        validationService.validateOrderRequest(request);
        return pricingService.calculateSummary(request);
    }

    @Transactional
    public Order createOrder(OrderRequestDTO request) {
        // Validate
        validationService.validateOrderRequest(request);

        // Calculate summary
        OrderSummary summary = pricingService.calculateSummary(request);

        // Create order
        Order order = new Order(
            request.getCustomer().getCpf(),
            request.getCustomer().getEmail(),
            request.getZipCode(),
            request.getRegion(),
            request.getShippingMethod(),
            request.getCoupon(),
            request.getPaymentMethod(),
            request.getInstallments() != null ? request.getInstallments() : 1,
            request.getClubTier()
        );

        order.setSummary(summary);

        // Add items
        for (OrderItemDTO item : request.getItems()) {
            OrderItem orderItem = new OrderItem(item.getSku(), item.getQuantity());
            order.getItems().add(orderItem);
        }

        // Save order
        return orderRepository.save(order);
    }

    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Order processPayment(String orderId, String transactionId, Instant paidAt) {
        Order order = getOrder(orderId);

        // Check if order is awaiting payment
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw new OrderException("OPERATION_NOT_ALLOWED", "Order is not awaiting payment", HttpStatus.CONFLICT);
        }

        // Check if payment is too late (more than 48 hours)
        Instant createdAt = order.getCreatedAt();
        Instant deadline = createdAt.plusSeconds(48 * 60 * 60);
        if (paidAt.isAfter(deadline)) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            notifyCustomer(order, NotificationType.ORDER_CANCELLED);
            throw new OrderException("ORDER_EXPIRED", "Payment is too late", HttpStatus.CONFLICT);
        }

        // Check if this is a duplicate payment (same transactionId)
        if (transactionId.equals(order.getLastTransactionId())) {
            // Already processed, return current state
            return order;
        }

        // Check if there's enough stock
        validateStock(order);

        // Process payment
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(paidAt);
        order.setLastTransactionId(transactionId);

        // Deduct from stock
        deductStock(order);

        // Add credit to customer
        addCreditToCustomer(order);

        // Save
        orderRepository.save(order);

        // Notify customer
        notifyCustomer(order, NotificationType.PAYMENT_APPROVED);

        return order;
    }

    @Transactional
    public Order shipOrder(String orderId, String trackingCode) {
        Order order = getOrder(orderId);

        if (order.getStatus() != OrderStatus.PAID) {
            throw new OrderException("OPERATION_NOT_ALLOWED", "Order is not paid", HttpStatus.CONFLICT);
        }

        order.setStatus(OrderStatus.SHIPPED);
        order.setTrackingCode(trackingCode);
        orderRepository.save(order);

        notifyCustomer(order, NotificationType.ORDER_SHIPPED);

        return order;
    }

    @Transactional
    public Order deliverOrder(String orderId) {
        Order order = getOrder(orderId);

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new OrderException("OPERATION_NOT_ALLOWED", "Order is not shipped", HttpStatus.CONFLICT);
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        return order;
    }

    @Transactional
    public Order cancelOrder(String orderId) {
        Order order = getOrder(orderId);

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new OrderException("OPERATION_NOT_ALLOWED", "Cannot cancel a shipped or delivered order", HttpStatus.CONFLICT);
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            // Already cancelled, just return
            return order;
        }

        // If paid, need to restore stock and credit
        if (order.getStatus() == OrderStatus.PAID) {
            restoreStock(order);
            removeCreditFromCustomer(order);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        notifyCustomer(order, NotificationType.ORDER_CANCELLED);

        return order;
    }

    private void validateStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Optional<Product> product = productRepository.findById(item.getSku());
            if (product.isEmpty() || product.get().getStock() < item.getQuantity()) {
                // Cancel order
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                notifyCustomer(order, NotificationType.ORDER_CANCELLED);
                throw new OrderException("INSUFFICIENT_STOCK", "Insufficient stock", HttpStatus.CONFLICT);
            }
        }
    }

    private void deductStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Optional<Product> product = productRepository.findById(item.getSku());
            if (product.isPresent()) {
                Product p = product.get();
                p.setStock(p.getStock() - item.getQuantity());
                productRepository.save(p);
            }
        }
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Optional<Product> product = productRepository.findById(item.getSku());
            if (product.isPresent()) {
                Product p = product.get();
                p.setStock(p.getStock() + item.getQuantity());
                productRepository.save(p);
            }
        }
    }

    private void addCreditToCustomer(Order order) {
        Optional<Customer> customerOpt = customerRepository.findById(order.getCustomerCpf());
        Customer customer = customerOpt.orElseGet(() -> new Customer(order.getCustomerCpf(), order.getCustomerEmail()));

        BigDecimal credit = order.getSummary().getNextPurchaseCredit();
        customer.setCreditBalance(PricingUtil.add(customer.getCreditBalance(), credit));

        customerRepository.save(customer);
    }

    private void removeCreditFromCustomer(Order order) {
        Optional<Customer> customerOpt = customerRepository.findById(order.getCustomerCpf());
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            BigDecimal credit = order.getSummary().getNextPurchaseCredit();
            customer.setCreditBalance(PricingUtil.subtract(customer.getCreditBalance(), credit));
            customerRepository.save(customer);
        }
    }

    private void notifyCustomer(Order order, NotificationType type) {
        CustomerNotification notification = new CustomerNotification(order.getCustomerCpf(), type, order.getId());
        notificationRepository.save(notification);
    }
}
