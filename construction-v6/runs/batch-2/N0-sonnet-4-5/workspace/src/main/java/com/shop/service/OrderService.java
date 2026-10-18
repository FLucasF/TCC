package com.shop.service;

import com.shop.config.Database;
import com.shop.dto.CheckoutRequest;
import com.shop.dto.CheckoutSummary;
import com.shop.exception.BusinessException;
import com.shop.model.Order;
import com.shop.model.OrderItem;
import com.shop.model.enums.NotificationType;
import com.shop.model.enums.OrderStatus;
import com.shop.repository.CustomerRepository;
import com.shop.repository.NotificationRepository;
import com.shop.repository.OrderRepository;
import com.shop.repository.ProductRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class OrderService {
    private final Database database;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final NotificationRepository notificationRepository;
    private final CheckoutService checkoutService;

    private final Map<String, PaymentResult> paymentCache = new HashMap<>();

    private static class PaymentResult {
        boolean success;
        String errorCode;
        Order order;

        PaymentResult(boolean success, String errorCode, Order order) {
            this.success = success;
            this.errorCode = errorCode;
            this.order = order;
        }
    }

    public OrderService(Database database, OrderRepository orderRepository,
                       ProductRepository productRepository, CustomerRepository customerRepository,
                       NotificationRepository notificationRepository, CheckoutService checkoutService) {
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

                customerRepository.create(conn, request.getCustomer().getCpf(), request.getCustomer().getEmail());

                Long orderId = orderRepository.create(conn, request.getCustomer().getCpf(), summary, request.getItems());

                conn.commit();

                return orderRepository.findById(orderId);
            } catch (BusinessException e) {
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

    public Order processPayment(Long orderId, String transactionId, Instant paidAt) {
        String cacheKey = orderId + ":" + transactionId;

        synchronized (paymentCache) {
            if (paymentCache.containsKey(cacheKey)) {
                PaymentResult cached = paymentCache.get(cacheKey);
                if (!cached.success) {
                    throw new BusinessException(cached.errorCode, 409);
                }
                return cached.order;
            }
        }

        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Order order = orderRepository.findByIdForUpdate(conn, orderId);
                if (order == null) {
                    throw new BusinessException("ORDER_NOT_FOUND", 404);
                }

                if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
                    throw new BusinessException("OPERATION_NOT_ALLOWED", 409);
                }

                Duration timeSinceCreation = Duration.between(order.getCreatedAt(), paidAt);
                if (timeSinceCreation.toHours() > 48) {
                    orderRepository.updateStatus(conn, orderId, OrderStatus.CANCELLED);
                    orderRepository.updatePaymentTransaction(conn, orderId, transactionId);
                    notificationRepository.create(conn, NotificationType.ORDER_CANCELLED, orderId, order.getCustomerCpf());
                    conn.commit();

                    Order cancelledOrder = orderRepository.findById(orderId);
                    synchronized (paymentCache) {
                        paymentCache.put(cacheKey, new PaymentResult(false, "ORDER_EXPIRED", cancelledOrder));
                    }
                    throw new BusinessException("ORDER_EXPIRED", 409);
                }

                try {
                    for (OrderItem item : order.getItems()) {
                        productRepository.decrementStock(conn, item.getSku(), item.getQuantity());
                    }
                } catch (SQLException e) {
                    orderRepository.updateStatus(conn, orderId, OrderStatus.CANCELLED);
                    orderRepository.updatePaymentTransaction(conn, orderId, transactionId);
                    notificationRepository.create(conn, NotificationType.ORDER_CANCELLED, orderId, order.getCustomerCpf());
                    conn.commit();

                    Order cancelledOrder = orderRepository.findById(orderId);
                    synchronized (paymentCache) {
                        paymentCache.put(cacheKey, new PaymentResult(false, "INSUFFICIENT_STOCK", cancelledOrder));
                    }
                    throw new BusinessException("INSUFFICIENT_STOCK", 409);
                }

                if (order.getSummary().getNextPurchaseCredit().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    customerRepository.addCredit(conn, order.getCustomerCpf(), order.getSummary().getNextPurchaseCredit());
                }

                orderRepository.updateStatus(conn, orderId, OrderStatus.PAID);
                orderRepository.updatePaymentTransaction(conn, orderId, transactionId);

                notificationRepository.create(conn, NotificationType.PAYMENT_APPROVED, orderId, order.getCustomerCpf());

                conn.commit();

                Order paidOrder = orderRepository.findById(orderId);
                synchronized (paymentCache) {
                    paymentCache.put(cacheKey, new PaymentResult(true, null, paidOrder));
                }
                return paidOrder;

            } catch (BusinessException e) {
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

    public Order shipOrder(Long orderId, String trackingCode) {
        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Order order = orderRepository.findByIdForUpdate(conn, orderId);
                if (order == null) {
                    throw new BusinessException("ORDER_NOT_FOUND", 404);
                }

                if (order.getStatus() != OrderStatus.PAID) {
                    throw new BusinessException("OPERATION_NOT_ALLOWED", 409);
                }

                orderRepository.updateStatus(conn, orderId, OrderStatus.SHIPPED);
                orderRepository.updateTrackingCode(conn, orderId, trackingCode);

                notificationRepository.create(conn, NotificationType.ORDER_SHIPPED, orderId, order.getCustomerCpf());

                conn.commit();
                return orderRepository.findById(orderId);
            } catch (BusinessException e) {
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

    public Order deliverOrder(Long orderId) {
        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Order order = orderRepository.findByIdForUpdate(conn, orderId);
                if (order == null) {
                    throw new BusinessException("ORDER_NOT_FOUND", 404);
                }

                if (order.getStatus() != OrderStatus.SHIPPED) {
                    throw new BusinessException("OPERATION_NOT_ALLOWED", 409);
                }

                orderRepository.updateStatus(conn, orderId, OrderStatus.DELIVERED);

                conn.commit();
                return orderRepository.findById(orderId);
            } catch (BusinessException e) {
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

    public Order cancelOrder(Long orderId) {
        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Order order = orderRepository.findByIdForUpdate(conn, orderId);
                if (order == null) {
                    throw new BusinessException("ORDER_NOT_FOUND", 404);
                }

                if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
                    throw new BusinessException("OPERATION_NOT_ALLOWED", 409);
                }

                if (order.getStatus() == OrderStatus.CANCELLED) {
                    conn.commit();
                    return order;
                }

                if (order.getStatus() == OrderStatus.PAID) {
                    for (OrderItem item : order.getItems()) {
                        productRepository.incrementStock(conn, item.getSku(), item.getQuantity());
                    }

                    if (order.getSummary().getNextPurchaseCredit().compareTo(java.math.BigDecimal.ZERO) > 0) {
                        customerRepository.subtractCredit(conn, order.getCustomerCpf(),
                                order.getSummary().getNextPurchaseCredit());
                    }
                }

                orderRepository.updateStatus(conn, orderId, OrderStatus.CANCELLED);
                notificationRepository.create(conn, NotificationType.ORDER_CANCELLED, orderId, order.getCustomerCpf());

                conn.commit();
                return orderRepository.findById(orderId);
            } catch (BusinessException e) {
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

    public Order getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new BusinessException("ORDER_NOT_FOUND", 404);
        }
        return order;
    }
}
