package com.loja.orderservice.service;

import com.loja.orderservice.dto.*;
import com.loja.orderservice.entity.*;
import com.loja.orderservice.exception.BusinessException;
import com.loja.orderservice.model.*;
import com.loja.orderservice.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class OrderService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final CheckoutCalculator checkoutCalculator;
    private final NotificationService notificationService;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        CustomerRepository customerRepository,
                        CheckoutCalculator checkoutCalculator,
                        NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.checkoutCalculator = checkoutCalculator;
        this.notificationService = notificationService;
    }

    public SummaryResponse calculateSummary(CheckoutRequest request) {
        OrderSummary summary = checkoutCalculator.calculate(request);
        return toSummaryResponse(summary);
    }

    @Transactional
    public OrderResponse createOrder(CheckoutRequest request) {
        OrderSummary summary = checkoutCalculator.calculate(request);

        Order order = new Order();
        order.setCustomerCpf(request.getCustomer().getCpf());
        order.setCustomerEmail(request.getCustomer().getEmail());
        order.setZipCode(request.getZipCode());
        order.setRegion(request.getRegion());
        order.setShippingMethod(request.getShippingMethod());
        order.setCoupon(request.getCoupon());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setClubTier(request.getClubTier());
        order.setSummary(summary);

        for (var item : request.getItems()) {
            order.getItems().add(new OrderItem(item.getSku(), item.getQuantity()));
        }

        Customer customer = customerRepository.findById(request.getCustomer().getCpf())
                .orElse(new Customer(request.getCustomer().getCpf()));
        customerRepository.save(customer);

        order = orderRepository.save(order);

        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND));
        return toOrderResponse(order);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public OrderResponse processPayment(Long id, PaymentRequest request) {
        if (request.getTransactionId() == null || request.getPaidAt() == null) {
            throw new BusinessException("INVALID_ORDER", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (order.getLastTransactionId() != null &&
            order.getLastTransactionId().equals(request.getTransactionId())) {
            return toOrderResponse(order);
        }

        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw new BusinessException("OPERATION_NOT_ALLOWED", HttpStatus.CONFLICT);
        }

        Duration elapsed = Duration.between(order.getCreatedAt(), request.getPaidAt());
        if (elapsed.compareTo(Duration.ofHours(48)) > 0) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setLastTransactionId(request.getTransactionId());
            orderRepository.save(order);
            notificationService.notifyCustomer(order.getCustomerCpf(), NotificationType.ORDER_CANCELLED, order.getId());
            throw new BusinessException("ORDER_EXPIRED", HttpStatus.CONFLICT);
        }

        try {
            reserveStock(order);
        } catch (BusinessException e) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setLastTransactionId(request.getTransactionId());
            orderRepository.save(order);
            notificationService.notifyCustomer(order.getCustomerCpf(), NotificationType.ORDER_CANCELLED, order.getId());
            throw e;
        }

        Customer customer = customerRepository.findByCpfForUpdate(order.getCustomerCpf())
                .orElse(new Customer(order.getCustomerCpf()));
        customer.addCredit(order.getSummary().getNextPurchaseCredit());
        customerRepository.save(customer);

        order.setStatus(OrderStatus.PAID);
        order.setLastTransactionId(request.getTransactionId());
        orderRepository.save(order);

        notificationService.notifyCustomer(order.getCustomerCpf(), NotificationType.PAYMENT_APPROVED, order.getId());

        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse shipOrder(Long id, ShipmentRequest request) {
        if (request.getTrackingCode() == null) {
            throw new BusinessException("INVALID_ORDER", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException("OPERATION_NOT_ALLOWED", HttpStatus.CONFLICT);
        }

        order.setStatus(OrderStatus.SHIPPED);
        order.setTrackingCode(request.getTrackingCode());
        orderRepository.save(order);

        notificationService.notifyCustomer(order.getCustomerCpf(), NotificationType.ORDER_SHIPPED, order.getId());

        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse deliverOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BusinessException("OPERATION_NOT_ALLOWED", HttpStatus.CONFLICT);
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessException("OPERATION_NOT_ALLOWED", HttpStatus.CONFLICT);
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            return toOrderResponse(order);
        }

        if (order.getStatus() == OrderStatus.PAID) {
            releaseStock(order);

            Customer customer = customerRepository.findByCpfForUpdate(order.getCustomerCpf())
                    .orElseThrow(() -> new BusinessException("CUSTOMER_NOT_FOUND", HttpStatus.NOT_FOUND));
            customer.subtractCredit(order.getSummary().getNextPurchaseCredit());
            customerRepository.save(customer);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        notificationService.notifyCustomer(order.getCustomerCpf(), NotificationType.ORDER_CANCELLED, order.getId());

        return toOrderResponse(order);
    }

    private void reserveStock(Order order) {
        List<Product> productsToUpdate = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findBySkuForUpdate(item.getSku())
                    .orElseThrow(() -> new BusinessException("UNKNOWN_PRODUCT", HttpStatus.UNPROCESSABLE_ENTITY));

            if (product.getStock() < item.getQuantity()) {
                throw new BusinessException("INSUFFICIENT_STOCK", HttpStatus.CONFLICT);
            }

            product.setStock(product.getStock() - item.getQuantity());
            productsToUpdate.add(product);
        }

        productRepository.saveAll(productsToUpdate);
    }

    private void releaseStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findBySkuForUpdate(item.getSku())
                    .orElseThrow(() -> new BusinessException("UNKNOWN_PRODUCT", HttpStatus.UNPROCESSABLE_ENTITY));

            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }
    }

    private SummaryResponse toSummaryResponse(OrderSummary summary) {
        SummaryResponse response = new SummaryResponse();
        response.setProductsSubtotal(summary.getProductsSubtotal());
        response.setCouponDiscount(summary.getCouponDiscount());
        response.setShippingCost(summary.getShippingCost());
        response.setDeliveryDays(summary.getDeliveryDays());
        response.setInsurance(summary.getInsurance());
        response.setPaymentAdjustment(summary.getPaymentAdjustment());
        response.setFinalTotal(summary.getFinalTotal());
        response.setInstallments(summary.getInstallments());
        response.setInstallmentAmount(summary.getInstallmentAmount());
        response.setNextPurchaseCredit(summary.getNextPurchaseCredit());
        response.setGift(summary.getGift());
        return response;
    }

    private OrderResponse toOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setSummary(toSummaryResponse(order.getSummary()));
        response.setTrackingCode(order.getTrackingCode());

        List<OrderResponse.ItemResponse> items = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            items.add(new OrderResponse.ItemResponse(item.getSku(), item.getQuantity()));
        }
        response.setItems(items);

        return response;
    }
}
