package com.loja.api;

import com.google.gson.Gson;
import com.loja.models.*;
import com.loja.services.*;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.sql.SQLException;

public class Routes {
    private static final Gson gson = new Gson();

    public static void setup(Javalin app) {
        app.post("/checkout/summary", Routes::checkoutSummary);
        app.post("/orders", Routes::createOrder);
        app.get("/orders/{id}", Routes::getOrder);
        app.post("/orders/{id}/payment", Routes::processPayment);
        app.post("/orders/{id}/shipment", Routes::shipOrder);
        app.post("/orders/{id}/delivery", Routes::deliverOrder);
        app.post("/orders/{id}/cancellation", Routes::cancelOrder);
        app.get("/products/{sku}", Routes::getProduct);
        app.get("/customers/{cpf}", Routes::getCustomer);
        app.get("/customers/{cpf}/notifications", Routes::getNotifications);
        app.post("/notifications/{id}/read", Routes::markNotificationAsRead);
    }

    private static void checkoutSummary(Context ctx) throws SQLException {
        CheckoutRequest req = gson.fromJson(ctx.body(), CheckoutRequest.class);
        CheckoutService.ValidationError validationError = CheckoutService.validateCheckoutRequest(req);

        if (validationError != null) {
            ctx.status(validationError.httpStatus);
            ctx.json(new ErrorResponse(validationError.code));
            return;
        }

        OrderSummary summary = CheckoutService.calculateSummary(req);
        ctx.status(200);
        ctx.json(summary);
    }

    private static void createOrder(Context ctx) throws SQLException {
        CheckoutRequest req = gson.fromJson(ctx.body(), CheckoutRequest.class);
        CheckoutService.ValidationError validationError = CheckoutService.validateCheckoutRequest(req);

        if (validationError != null) {
            ctx.status(validationError.httpStatus);
            ctx.json(new ErrorResponse(validationError.code));
            return;
        }

        OrderSummary summary = CheckoutService.calculateSummary(req);
        Order order = OrderService.createOrder(req, summary);

        ctx.status(201);
        ctx.json(order);
    }

    private static void getOrder(Context ctx) throws SQLException {
        String orderId = ctx.pathParam("id");
        Order order = OrderService.getOrder(orderId);

        if (order == null) {
            ctx.status(404);
            ctx.json(new ErrorResponse("ORDER_NOT_FOUND"));
            return;
        }

        ctx.status(200);
        ctx.json(order);
    }

    private static void processPayment(Context ctx) throws SQLException {
        String orderId = ctx.pathParam("id");
        PaymentRequest paymentReq = gson.fromJson(ctx.body(), PaymentRequest.class);

        PaymentService.PaymentResult result = PaymentService.processPayment(orderId, paymentReq);

        if (!result.success) {
            ctx.status(result.httpStatus);
            ctx.json(new ErrorResponse(result.error));
            return;
        }

        ctx.status(200);
        ctx.json(result.order);
    }

    private static void shipOrder(Context ctx) throws SQLException {
        String orderId = ctx.pathParam("id");
        ShipmentRequest shipmentReq = gson.fromJson(ctx.body(), ShipmentRequest.class);

        Order order = OrderService.getOrder(orderId);
        if (order == null) {
            ctx.status(404);
            ctx.json(new ErrorResponse("ORDER_NOT_FOUND"));
            return;
        }

        if (shipmentReq.trackingCode == null || shipmentReq.trackingCode.isEmpty()) {
            ctx.status(422);
            ctx.json(new ErrorResponse("INVALID_ORDER"));
            return;
        }

        if (order.status != Enums.OrderStatus.PAID) {
            ctx.status(409);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED"));
            return;
        }

        OrderService.updateOrderTracking(orderId, shipmentReq.trackingCode);
        OrderService.updateOrderStatus(orderId, Enums.OrderStatus.SHIPPED);

        String cpf = OrderService.getCustomerCpf(orderId);
        NotificationService.createNotification(cpf, Enums.NotificationType.ORDER_SHIPPED, orderId);

        ctx.status(200);
        ctx.json(OrderService.getOrder(orderId));
    }

    private static void deliverOrder(Context ctx) throws SQLException {
        String orderId = ctx.pathParam("id");
        Order order = OrderService.getOrder(orderId);

        if (order == null) {
            ctx.status(404);
            ctx.json(new ErrorResponse("ORDER_NOT_FOUND"));
            return;
        }

        if (order.status != Enums.OrderStatus.SHIPPED) {
            ctx.status(409);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED"));
            return;
        }

        OrderService.updateOrderStatus(orderId, Enums.OrderStatus.DELIVERED);

        ctx.status(200);
        ctx.json(OrderService.getOrder(orderId));
    }

    private static void cancelOrder(Context ctx) throws SQLException {
        String orderId = ctx.pathParam("id");
        Order order = OrderService.getOrder(orderId);

        if (order == null) {
            ctx.status(404);
            ctx.json(new ErrorResponse("ORDER_NOT_FOUND"));
            return;
        }

        if (order.status == Enums.OrderStatus.SHIPPED || order.status == Enums.OrderStatus.DELIVERED) {
            ctx.status(409);
            ctx.json(new ErrorResponse("OPERATION_NOT_ALLOWED"));
            return;
        }

        String cpf = OrderService.getCustomerCpf(orderId);

        if (order.status == Enums.OrderStatus.PAID) {
            for (OrderItem item : order.items) {
                ProductService.updateStock(item.sku, item.quantity);
            }
            CustomerService.updateCreditBalance(cpf, -order.summary.nextPurchaseCredit);
        }

        OrderService.updateOrderStatus(orderId, Enums.OrderStatus.CANCELLED);
        NotificationService.createNotification(cpf, Enums.NotificationType.ORDER_CANCELLED, orderId);

        ctx.status(200);
        ctx.json(OrderService.getOrder(orderId));
    }

    private static void getProduct(Context ctx) throws SQLException {
        String sku = ctx.pathParam("sku");
        Product product = ProductService.getProduct(sku);

        if (product == null) {
            ctx.status(404);
            ctx.json(new ErrorResponse("PRODUCT_NOT_FOUND"));
            return;
        }

        ctx.status(200);
        ctx.json(product);
    }

    private static void getCustomer(Context ctx) throws SQLException {
        String cpf = ctx.pathParam("cpf");
        Customer customer = CustomerService.getCustomer(cpf);

        if (customer == null) {
            ctx.status(404);
            ctx.json(new ErrorResponse("CUSTOMER_NOT_FOUND"));
            return;
        }

        ctx.status(200);
        ctx.json(customer);
    }

    private static void getNotifications(Context ctx) throws SQLException {
        String cpf = ctx.pathParam("cpf");
        Customer customer = CustomerService.getCustomer(cpf);

        if (customer == null) {
            ctx.status(404);
            ctx.json(new ErrorResponse("CUSTOMER_NOT_FOUND"));
            return;
        }

        ctx.status(200);
        ctx.json(NotificationService.getCustomerNotifications(cpf));
    }

    private static void markNotificationAsRead(Context ctx) throws SQLException {
        String notificationId = ctx.pathParam("id");
        Notification notification = NotificationService.getNotification(notificationId);

        if (notification == null) {
            ctx.status(404);
            ctx.json(new ErrorResponse("NOTIFICATION_NOT_FOUND"));
            return;
        }

        NotificationService.markAsRead(notificationId);
        Notification updated = NotificationService.getNotification(notificationId);

        ctx.status(200);
        ctx.json(updated);
    }
}
