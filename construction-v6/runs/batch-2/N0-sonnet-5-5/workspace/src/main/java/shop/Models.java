package shop;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

enum OrderStatus { AWAITING_PAYMENT, PAID, SHIPPED, DELIVERED, CANCELLED }

enum NotificationType { PAYMENT_APPROVED, ORDER_SHIPPED, ORDER_CANCELLED }

enum NotificationStatus { NEW, READ }

record Product(String sku, String name, BigDecimal price, BigDecimal weightKg, int stock, boolean discontinued) {}

record OrderItem(String sku, int quantity) {}

record Customer(String cpf, BigDecimal creditBalance) {}

record Notification(long id, String cpf, NotificationType type, String orderId, NotificationStatus status) {}

/** Resposta registrada para um aviso de pagamento; errorCode nulo significa que deu certo. */
record PaymentRecord(String errorCode, int httpStatus) {}

record Summary(BigDecimal productsSubtotal, BigDecimal couponDiscount, BigDecimal shippingCost, int deliveryDays,
               BigDecimal insurance, BigDecimal paymentAdjustment, BigDecimal finalTotal, int installments,
               BigDecimal installmentAmount, BigDecimal nextPurchaseCredit, boolean gift) {
    Summary {
        productsSubtotal = Money.round(productsSubtotal);
        couponDiscount = Money.round(couponDiscount);
        shippingCost = Money.round(shippingCost);
        insurance = Money.round(insurance);
        paymentAdjustment = Money.round(paymentAdjustment);
        finalTotal = Money.round(finalTotal);
        installmentAmount = Money.round(installmentAmount);
        nextPurchaseCredit = Money.round(nextPurchaseCredit);
    }
}

record Order(String id, String cpf, String email, OrderStatus status, Instant createdAt, Summary summary,
             List<OrderItem> items, String trackingCode, Instant paidAt, String transactionId) {
    Order withStatus(OrderStatus s) {
        return new Order(id, cpf, email, s, createdAt, summary, items, trackingCode, paidAt, transactionId);
    }

    Order paid(String txId, Instant at) {
        return new Order(id, cpf, email, OrderStatus.PAID, createdAt, summary, items, trackingCode, at, txId);
    }

    Order shipped(String tracking) {
        return new Order(id, cpf, email, OrderStatus.SHIPPED, createdAt, summary, items, tracking, paidAt, transactionId);
    }
}
