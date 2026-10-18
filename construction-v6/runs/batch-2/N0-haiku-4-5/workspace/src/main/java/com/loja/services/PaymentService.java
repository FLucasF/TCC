package com.loja.services;

import com.loja.api.PaymentRequest;
import com.loja.models.Enums;
import com.loja.models.Order;
import com.loja.models.OrderItem;

import java.sql.SQLException;
import java.time.Instant;

public class PaymentService {
    private static final long PAYMENT_TIMEOUT_MS = 48 * 60 * 60 * 1000;

    public static class PaymentResult {
        public String error;
        public int httpStatus;
        public Order order;
        public boolean success;

        public PaymentResult(String error, int httpStatus) {
            this.error = error;
            this.httpStatus = httpStatus;
            this.success = false;
        }

        public PaymentResult(Order order) {
            this.order = order;
            this.success = true;
        }
    }

    public static PaymentResult processPayment(String orderId, PaymentRequest paymentReq) throws SQLException {
        Order order = OrderService.getOrder(orderId);
        if (order == null) {
            return new PaymentResult("ORDER_NOT_FOUND", 404);
        }

        if (paymentReq.transactionId == null || paymentReq.transactionId.isEmpty() ||
            paymentReq.paidAt == null || paymentReq.paidAt.isEmpty()) {
            return new PaymentResult("INVALID_ORDER", 422);
        }

        if (order.status != Enums.OrderStatus.AWAITING_PAYMENT) {
            if (OrderService.isPaymentDuplicate(orderId, paymentReq.transactionId)) {
                return new PaymentResult(order);
            }
            return new PaymentResult("OPERATION_NOT_ALLOWED", 409);
        }

        Instant paidAt = Instant.parse(paymentReq.paidAt);
        Instant createdAt = order.createdAt;
        long timeDiffMs = paidAt.toEpochMilli() - createdAt.toEpochMilli();

        if (timeDiffMs > PAYMENT_TIMEOUT_MS) {
            OrderService.updateOrderStatus(orderId, Enums.OrderStatus.CANCELLED);
            String cpf = OrderService.getCustomerCpf(orderId);
            NotificationService.createNotification(cpf, Enums.NotificationType.ORDER_CANCELLED, orderId);
            return new PaymentResult("ORDER_EXPIRED", 409);
        }

        for (OrderItem item : order.items) {
            if (ProductService.getProduct(item.sku).stock < item.quantity) {
                OrderService.updateOrderStatus(orderId, Enums.OrderStatus.CANCELLED);
                String cpf = OrderService.getCustomerCpf(orderId);
                NotificationService.createNotification(cpf, Enums.NotificationType.ORDER_CANCELLED, orderId);
                return new PaymentResult("INSUFFICIENT_STOCK", 409);
            }
        }

        for (OrderItem item : order.items) {
            ProductService.updateStock(item.sku, -item.quantity);
        }

        String cpf = OrderService.getCustomerCpf(orderId);
        CustomerService.updateCreditBalance(cpf, order.summary.nextPurchaseCredit);

        OrderService.recordPayment(orderId, paymentReq.transactionId, paidAt);
        OrderService.updateOrderStatus(orderId, Enums.OrderStatus.PAID);

        NotificationService.createNotification(cpf, Enums.NotificationType.PAYMENT_APPROVED, orderId);

        return new PaymentResult(OrderService.getOrder(orderId));
    }
}
