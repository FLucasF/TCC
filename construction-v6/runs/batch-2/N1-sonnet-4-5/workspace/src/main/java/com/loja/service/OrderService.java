package com.loja.service;

import com.loja.domain.*;
import com.loja.repository.*;
import com.loja.util.ApiException;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class OrderService {
    private final Database database;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final NotificationRepository notificationRepository;
    private final CheckoutService checkoutService;

    public OrderService(Database database,
                       OrderRepository orderRepository,
                       ProductRepository productRepository,
                       CustomerRepository customerRepository,
                       NotificationRepository notificationRepository,
                       CheckoutService checkoutService) {
        this.database = database;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.notificationRepository = notificationRepository;
        this.checkoutService = checkoutService;
    }

    public Order createOrder(CheckoutRequest request) {
        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                CheckoutSummary summary = checkoutService.calculateSummary(request, conn);

                customerRepository.create(request.getCustomer().getCpf(), request.getCustomer().getEmail(), conn);

                Order order = new Order();
                order.setId(UUID.randomUUID().toString());
                order.setCustomerCpf(request.getCustomer().getCpf());
                order.setCustomerEmail(request.getCustomer().getEmail());
                order.setItems(new ArrayList<>(request.getItems()));
                order.setZipCode(request.getZipCode());
                order.setRegion(request.getRegion());
                order.setShippingMethod(request.getShippingMethod());
                order.setCoupon(request.getCoupon());
                order.setPaymentMethod(request.getPaymentMethod());
                order.setClubTier(request.getClubTier());
                order.setStatus(OrderStatus.AWAITING_PAYMENT);
                order.setCreatedAt(Instant.now());
                order.setSummary(summary);

                orderRepository.save(order, conn);

                conn.commit();
                return order;
            } catch (ApiException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to create order", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database connection failed", e);
        }
    }

    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", 404));
    }

    public Order processPayment(String orderId, String transactionId, Instant paidAt) {
        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Optional<Order> existingOrder = orderRepository.findByIdForUpdate(orderId, conn);
                if (existingOrder.isEmpty()) {
                    conn.rollback();
                    throw new ApiException("ORDER_NOT_FOUND", 404);
                }

                Order order = existingOrder.get();

                if (transactionId == null || transactionId.isEmpty() || paidAt == null) {
                    conn.rollback();
                    throw new ApiException("INVALID_ORDER", 422);
                }

                boolean isNewTransaction = orderRepository.recordPaymentTransaction(orderId, transactionId, paidAt, conn);

                if (!isNewTransaction) {
                    conn.rollback();
                    return order;
                }

                if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
                    conn.rollback();
                    throw new ApiException("OPERATION_NOT_ALLOWED", 409);
                }

                Duration elapsed = Duration.between(order.getCreatedAt(), paidAt);
                if (elapsed.compareTo(Duration.ofHours(48)) > 0) {
                    orderRepository.updateStatus(orderId, OrderStatus.CANCELLED, conn);
                    order.setStatus(OrderStatus.CANCELLED);
                    notificationRepository.create(order.getCustomerCpf(), NotificationType.ORDER_CANCELLED, orderId, conn);
                    conn.commit();
                    throw new ApiException("ORDER_EXPIRED", 409);
                }

                Map<String, Integer> skuQuantities = new HashMap<>();
                for (OrderItem item : order.getItems()) {
                    skuQuantities.put(item.getSku(), item.getQuantity());
                }

                try {
                    productRepository.decreaseStock(skuQuantities, conn);
                } catch (SQLException e) {
                    if (e.getMessage().equals("INSUFFICIENT_STOCK")) {
                        orderRepository.updateStatus(orderId, OrderStatus.CANCELLED, conn);
                        order.setStatus(OrderStatus.CANCELLED);
                        notificationRepository.create(order.getCustomerCpf(), NotificationType.ORDER_CANCELLED, orderId, conn);
                        conn.commit();
                        throw new ApiException("INSUFFICIENT_STOCK", 409);
                    }
                    throw e;
                }

                if (order.getSummary().getNextPurchaseCredit().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    customerRepository.addCredit(order.getCustomerCpf(), order.getSummary().getNextPurchaseCredit(), conn);
                }

                orderRepository.updateStatus(orderId, OrderStatus.PAID, conn);
                orderRepository.updatePaymentTransaction(orderId, transactionId, conn);
                order.setStatus(OrderStatus.PAID);
                order.setPaymentTransactionId(transactionId);

                notificationRepository.create(order.getCustomerCpf(), NotificationType.PAYMENT_APPROVED, orderId, conn);

                conn.commit();
                return order;
            } catch (ApiException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to process payment", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database connection failed", e);
        }
    }

    public Order shipOrder(String orderId, String trackingCode) {
        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Optional<Order> existingOrder = orderRepository.findByIdForUpdate(orderId, conn);
                if (existingOrder.isEmpty()) {
                    conn.rollback();
                    throw new ApiException("ORDER_NOT_FOUND", 404);
                }

                Order order = existingOrder.get();

                if (trackingCode == null || trackingCode.isEmpty()) {
                    conn.rollback();
                    throw new ApiException("INVALID_ORDER", 422);
                }

                if (order.getStatus() != OrderStatus.PAID) {
                    conn.rollback();
                    throw new ApiException("OPERATION_NOT_ALLOWED", 409);
                }

                orderRepository.updateStatus(orderId, OrderStatus.SHIPPED, conn);
                orderRepository.updateTrackingCode(orderId, trackingCode, conn);
                order.setStatus(OrderStatus.SHIPPED);
                order.setTrackingCode(trackingCode);

                notificationRepository.create(order.getCustomerCpf(), NotificationType.ORDER_SHIPPED, orderId, conn);

                conn.commit();
                return order;
            } catch (ApiException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to ship order", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database connection failed", e);
        }
    }

    public Order deliverOrder(String orderId) {
        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Optional<Order> existingOrder = orderRepository.findByIdForUpdate(orderId, conn);
                if (existingOrder.isEmpty()) {
                    conn.rollback();
                    throw new ApiException("ORDER_NOT_FOUND", 404);
                }

                Order order = existingOrder.get();

                if (order.getStatus() != OrderStatus.SHIPPED) {
                    conn.rollback();
                    throw new ApiException("OPERATION_NOT_ALLOWED", 409);
                }

                orderRepository.updateStatus(orderId, OrderStatus.DELIVERED, conn);
                order.setStatus(OrderStatus.DELIVERED);

                conn.commit();
                return order;
            } catch (ApiException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to deliver order", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database connection failed", e);
        }
    }

    public Order cancelOrder(String orderId) {
        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Optional<Order> existingOrder = orderRepository.findByIdForUpdate(orderId, conn);
                if (existingOrder.isEmpty()) {
                    conn.rollback();
                    throw new ApiException("ORDER_NOT_FOUND", 404);
                }

                Order order = existingOrder.get();

                if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
                    conn.rollback();
                    throw new ApiException("OPERATION_NOT_ALLOWED", 409);
                }

                if (order.getStatus() == OrderStatus.PAID) {
                    Map<String, Integer> skuQuantities = new HashMap<>();
                    for (OrderItem item : order.getItems()) {
                        skuQuantities.put(item.getSku(), item.getQuantity());
                    }
                    productRepository.increaseStock(skuQuantities, conn);

                    if (order.getSummary().getNextPurchaseCredit().compareTo(java.math.BigDecimal.ZERO) > 0) {
                        customerRepository.subtractCredit(order.getCustomerCpf(), order.getSummary().getNextPurchaseCredit(), conn);
                    }
                }

                orderRepository.updateStatus(orderId, OrderStatus.CANCELLED, conn);
                order.setStatus(OrderStatus.CANCELLED);

                notificationRepository.create(order.getCustomerCpf(), NotificationType.ORDER_CANCELLED, orderId, conn);

                conn.commit();
                return order;
            } catch (ApiException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Failed to cancel order", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database connection failed", e);
        }
    }
}
