package com.clothing;

import com.clothing.model.*;
import com.clothing.service.*;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;

public class OrderRoutes {
    private static final Logger logger = LoggerFactory.getLogger(OrderRoutes.class);
    private final Javalin app;
    private final ProductRepository productRepo = new ProductRepository();
    private final CustomerRepository customerRepo = new CustomerRepository();
    private final OrderRepository orderRepo = new OrderRepository();
    private final CheckoutService checkoutService = new CheckoutService(productRepo, customerRepo, orderRepo);

    public OrderRoutes(Javalin app) {
        this.app = app;
    }

    public void register() {
        app.post("/checkout/summary", ctx -> {
            try {
                CheckoutRequest req = ctx.bodyAsClass(CheckoutRequest.class);
                var validationError = Validation.validateCheckout(req, productRepo);
                if (validationError != null) {
                    ctx.status(validationError.httpStatus());
                    ctx.json(new ErrorResponse(validationError.code()));
                    return;
                }

                if (req.coupon() != null && !req.coupon().isEmpty()) {
                    var couponError = Validation.validateCoupon(req.coupon(), req, productRepo, customerRepo);
                    if (couponError != null) {
                        ctx.status(couponError.httpStatus());
                        ctx.json(new ErrorResponse(couponError.code()));
                        return;
                    }
                }

                Summary summary = checkoutService.calculateSummary(req);

                var paymentError = Validation.validatePaymentMethodForTotal(req.paymentMethod(), summary.finalTotal());
                if (paymentError != null) {
                    ctx.status(paymentError.httpStatus());
                    ctx.json(new ErrorResponse(paymentError.code()));
                    return;
                }

                ctx.status(200);
                ctx.json(summary);
            } catch (Exception e) {
                logger.error("Error in checkout summary", e);
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR"));
            }
        });

        app.post("/orders", ctx -> {
            try {
                CheckoutRequest req = ctx.bodyAsClass(CheckoutRequest.class);
                var validationError = Validation.validateCheckout(req, productRepo);
                if (validationError != null) {
                    ctx.status(validationError.httpStatus());
                    ctx.json(new ErrorResponse(validationError.code()));
                    return;
                }

                if (req.coupon() != null && !req.coupon().isEmpty()) {
                    var couponError = Validation.validateCoupon(req.coupon(), req, productRepo, customerRepo);
                    if (couponError != null) {
                        ctx.status(couponError.httpStatus());
                        ctx.json(new ErrorResponse(couponError.code()));
                        return;
                    }
                }

                Summary summary = checkoutService.calculateSummary(req);

                var paymentError = Validation.validatePaymentMethodForTotal(req.paymentMethod(), summary.finalTotal());
                if (paymentError != null) {
                    ctx.status(paymentError.httpStatus());
                    ctx.json(new ErrorResponse(paymentError.code()));
                    return;
                }

                Order order = checkoutService.registerOrder(req);
                ctx.status(201);
                ctx.json(order);
            } catch (Exception e) {
                logger.error("Error creating order", e);
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR"));
            }
        });

        app.get("/orders/{id}", ctx -> {
            try {
                long orderId = Long.parseLong(ctx.pathParam("id"));
                Order order = orderRepo.getOrder(orderId);
                if (order == null) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("ORDER_NOT_FOUND"));
                    return;
                }
                ctx.status(200);
                ctx.json(order);
            } catch (Exception e) {
                logger.error("Error getting order", e);
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR"));
            }
        });

        app.post("/orders/{id}/payment", ctx -> {
            try {
                long orderId = Long.parseLong(ctx.pathParam("id"));
                PaymentRequest paymentReq = ctx.bodyAsClass(PaymentRequest.class);

                if (paymentReq.transactionId() == null || paymentReq.paidAt() == null) {
                    ctx.status(422);
                    ctx.json(new ErrorResponse("INVALID_ORDER"));
                    return;
                }

                Order order = orderRepo.getOrder(orderId);
                if (order == null) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("ORDER_NOT_FOUND"));
                    return;
                }

                if (order.status() != OrderStatus.AWAITING_PAYMENT) {
                    ctx.status(409);
                    ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED"));
                    return;
                }

                if (orderRepo.isPaymentAlreadyRecorded(orderId, paymentReq.transactionId())) {
                    ctx.status(200);
                    ctx.json(order);
                    return;
                }

                Instant paidAt = Instant.parse(paymentReq.paidAt());

                // Check for stock availability
                String cpf = orderRepo.getCustomerCpfForOrder(orderId);
                for (OrderItem item : order.items()) {
                    int currentStock = orderRepo.getStockForProduct(item.sku());
                    if (currentStock < item.quantity()) {
                        orderRepo.updateOrderStatus(orderId, OrderStatus.CANCELLED);
                        customerRepo.createNotification(cpf, orderId, NotificationType.ORDER_CANCELLED);
                        ctx.status(409);
                        ctx.json(new ErrorResponse("INSUFFICIENT_STOCK"));
                        return;
                    }
                }

                try {
                    checkoutService.processPayment(orderId, paymentReq.transactionId(), paidAt, order, cpf);
                    Order updatedOrder = orderRepo.getOrder(orderId);
                    ctx.status(200);
                    ctx.json(updatedOrder);
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().equals("ORDER_EXPIRED")) {
                        ctx.status(409);
                        ctx.json(new ErrorResponse("ORDER_EXPIRED"));
                        return;
                    }
                    throw e;
                }
            } catch (Exception e) {
                logger.error("Error processing payment", e);
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR"));
            }
        });

        app.post("/orders/{id}/shipment", ctx -> {
            try {
                long orderId = Long.parseLong(ctx.pathParam("id"));
                ShipmentRequest shipmentReq = ctx.bodyAsClass(ShipmentRequest.class);

                if (shipmentReq.trackingCode() == null) {
                    ctx.status(422);
                    ctx.json(new ErrorResponse("INVALID_ORDER"));
                    return;
                }

                Order order = orderRepo.getOrder(orderId);
                if (order == null) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("ORDER_NOT_FOUND"));
                    return;
                }

                if (order.status() != OrderStatus.PAID) {
                    ctx.status(409);
                    ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED"));
                    return;
                }

                orderRepo.setTrackingCode(orderId, shipmentReq.trackingCode());
                orderRepo.updateOrderStatusAndTimestamp(orderId, OrderStatus.SHIPPED, "shipped_at");

                Order updatedOrder = orderRepo.getOrder(orderId);
                ctx.status(200);
                ctx.json(updatedOrder);
            } catch (Exception e) {
                logger.error("Error shipping order", e);
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR"));
            }
        });

        app.post("/orders/{id}/delivery", ctx -> {
            try {
                long orderId = Long.parseLong(ctx.pathParam("id"));

                Order order = orderRepo.getOrder(orderId);
                if (order == null) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("ORDER_NOT_FOUND"));
                    return;
                }

                if (order.status() != OrderStatus.SHIPPED) {
                    ctx.status(409);
                    ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED"));
                    return;
                }

                orderRepo.updateOrderStatusAndTimestamp(orderId, OrderStatus.DELIVERED, "delivered_at");

                Order updatedOrder = orderRepo.getOrder(orderId);
                ctx.status(200);
                ctx.json(updatedOrder);
            } catch (Exception e) {
                logger.error("Error delivering order", e);
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR"));
            }
        });

        app.post("/orders/{id}/cancellation", ctx -> {
            try {
                long orderId = Long.parseLong(ctx.pathParam("id"));

                Order order = orderRepo.getOrder(orderId);
                if (order == null) {
                    ctx.status(404);
                    ctx.json(new ErrorResponse("ORDER_NOT_FOUND"));
                    return;
                }

                if (order.status() == OrderStatus.SHIPPED || order.status() == OrderStatus.DELIVERED) {
                    ctx.status(409);
                    ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED"));
                    return;
                }

                String cpf = orderRepo.getCustomerCpfForOrder(orderId);

                if (order.status() == OrderStatus.PAID) {
                    for (OrderItem item : order.items()) {
                        orderRepo.incrementStock(item.sku(), item.quantity());
                    }

                    BigDecimal credit = order.summary().nextPurchaseCredit();
                    if (credit.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        customerRepo.updateCreditBalance(cpf, credit.negate());
                    }
                }

                orderRepo.updateOrderStatus(orderId, OrderStatus.CANCELLED);
                customerRepo.createNotification(cpf, orderId, NotificationType.ORDER_CANCELLED);

                Order updatedOrder = orderRepo.getOrder(orderId);
                ctx.status(200);
                ctx.json(updatedOrder);
            } catch (Exception e) {
                logger.error("Error cancelling order", e);
                ctx.status(500);
                ctx.json(new ErrorResponse("INTERNAL_ERROR"));
            }
        });
    }
}
