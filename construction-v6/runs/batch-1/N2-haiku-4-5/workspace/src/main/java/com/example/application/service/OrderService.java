package com.example.application.service;

import com.example.api.dto.*;
import com.example.application.exception.*;
import com.example.domain.entity.*;
import com.example.domain.enums.*;
import com.example.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final CustomerService customerService;
    private final NotificationService notificationService;
    private final OrderSummaryCalculator summaryCalculator;
    private final ValidationService validationService;
    private final CouponValidator couponValidator;
    private final TransLogService transLogService;

    private static final long PAYMENT_TIMEOUT = 48 * 60 * 60 * 1000;

    public OrderService(
            OrderRepository orderRepository,
            ProductService productService,
            CustomerService customerService,
            NotificationService notificationService,
            OrderSummaryCalculator summaryCalculator,
            TransLogService transLogService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
        this.customerService = customerService;
        this.notificationService = notificationService;
        this.summaryCalculator = summaryCalculator;
        this.validationService = new ValidationService(productService);
        this.couponValidator = new CouponValidator(orderRepository);
        this.transLogService = transLogService;
    }

    public OrderSummary calculateSummary(CheckoutRequestDto request) {
        validationService.validateCheckout(request);
        validateAndAdjustForTransLog(request);
        BigDecimal productsSubtotal = summaryCalculator.calculateProductsSubtotal(request);
        couponValidator.validate(request, productsSubtotal);
        validatePaymentMethod(request, productsSubtotal);

        return summaryCalculator.calculate(request);
    }

    public Order createOrder(CheckoutRequestDto request) {
        OrderSummary summary = calculateSummary(request);

        Order order = new Order(request.getCustomer().getCpf(), request.getCustomer().getEmail(), summary);

        for (var item : request.getItems()) {
            order.addItem(new OrderItem(item.getSku(), item.getQuantity()));
        }

        return orderRepository.save(order);
    }

    public Order getOrder(String orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            throw new OrderNotFoundException();
        }
        return order;
    }

    public Order processPayment(String orderId, PaymentRequestDto paymentRequest) {
        if (paymentRequest.getTransactionId() == null || paymentRequest.getPaidAt() == null) {
            throw new InvalidOrderException();
        }

        Order order = getOrder(orderId);

        if (order.getLastSuccessfulTransactionId() != null &&
            order.getLastSuccessfulTransactionId().equals(paymentRequest.getTransactionId())) {
            return order;
        }

        if (!order.getStatus().equals(OrderStatus.AWAITING_PAYMENT)) {
            throw new OperationNotAllowedException();
        }

        if (!order.isPaidWithinTimeout(PAYMENT_TIMEOUT)) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            notificationService.createNotification(order.getCustomerCpf(), NotificationType.ORDER_CANCELLED, orderId);
            throw new OrderExpiredException();
        }

        if (!validateStockForPayment(order)) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            notificationService.createNotification(order.getCustomerCpf(), NotificationType.ORDER_CANCELLED, orderId);
            throw new InsufficientStockException();
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(paymentRequest.getPaidAt());
        order.setLastSuccessfulTransactionId(paymentRequest.getTransactionId());
        orderRepository.save(order);

        decreaseStock(order);
        addCreditToCustomer(order);

        notificationService.createNotification(order.getCustomerCpf(), NotificationType.PAYMENT_APPROVED, orderId);

        return order;
    }

    public Order shipOrder(String orderId, ShipmentRequestDto shipmentRequest) {
        if (shipmentRequest.getTrackingCode() == null) {
            throw new InvalidOrderException();
        }

        Order order = getOrder(orderId);

        if (!order.getStatus().equals(OrderStatus.PAID)) {
            throw new OperationNotAllowedException();
        }

        order.setStatus(OrderStatus.SHIPPED);
        order.setTrackingCode(shipmentRequest.getTrackingCode());
        orderRepository.save(order);

        notificationService.createNotification(order.getCustomerCpf(), NotificationType.ORDER_SHIPPED, orderId);

        return order;
    }

    public Order deliverOrder(String orderId) {
        Order order = getOrder(orderId);

        if (!order.getStatus().equals(OrderStatus.SHIPPED)) {
            throw new OperationNotAllowedException();
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        return order;
    }

    public Order cancelOrder(String orderId) {
        Order order = getOrder(orderId);

        if (order.getStatus().equals(OrderStatus.SHIPPED) || order.getStatus().equals(OrderStatus.DELIVERED)) {
            throw new OperationNotAllowedException();
        }

        if (order.getStatus().equals(OrderStatus.PAID)) {
            increaseStock(order);
            subtractCreditFromCustomer(order);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        notificationService.createNotification(order.getCustomerCpf(), NotificationType.ORDER_CANCELLED, orderId);

        return order;
    }

    private void validateAndAdjustForTransLog(CheckoutRequestDto request) {
        ShippingMethod method = ShippingMethod.valueOf(request.getShippingMethod());
        if (method == ShippingMethod.ECONOMY || method == ShippingMethod.EXPRESS) {
            OrderSummaryCalculator.ShippingQuote quote = transLogService.getQuote(
                    request.getZipCode(),
                    calculateTotalWeight(request),
                    method
            );
            if (quote == null) {
                throw new ShippingMethodUnavailableException();
            }
        }
    }

    private void validatePaymentMethod(CheckoutRequestDto request, BigDecimal productsSubtotal) {
        PaymentMethod method = PaymentMethod.valueOf(request.getPaymentMethod());
        if (method == PaymentMethod.BANK_SLIP) {
            OrderSummary summary = summaryCalculator.calculate(request);
            if (summary.getFinalTotal().compareTo(new BigDecimal("1000")) > 0) {
                throw new PaymentMethodUnavailableException();
            }
        }
    }

    private boolean validateStockForPayment(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = productService.getProduct(item.getSku());
            if (product == null || product.getStock() < item.getQuantity()) {
                return false;
            }
        }
        return true;
    }

    private void decreaseStock(Order order) {
        for (OrderItem item : order.getItems()) {
            productService.updateStock(item.getSku(), -item.getQuantity());
        }
    }

    private void increaseStock(Order order) {
        for (OrderItem item : order.getItems()) {
            productService.updateStock(item.getSku(), item.getQuantity());
        }
    }

    private void addCreditToCustomer(Order order) {
        customerService.addCredit(order.getCustomerCpf(), order.getSummary().getNextPurchaseCredit());
    }

    private void subtractCreditFromCustomer(Order order) {
        customerService.subtractCredit(order.getCustomerCpf(), order.getSummary().getNextPurchaseCredit());
    }

    private BigDecimal calculateTotalWeight(CheckoutRequestDto request) {
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (var item : request.getItems()) {
            Product product = productService.getProduct(item.getSku());
            BigDecimal itemWeight = product.getWeightKg().multiply(new BigDecimal(item.getQuantity()));
            totalWeight = totalWeight.add(itemWeight);
        }
        return totalWeight;
    }
}
