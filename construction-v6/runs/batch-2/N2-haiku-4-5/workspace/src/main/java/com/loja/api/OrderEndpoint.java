package com.loja.api;

import io.javalin.Javalin;
import com.loja.model.*;
import com.loja.service.OrderService;
import com.loja.service.CheckoutException;
import com.loja.service.OrderException;

public class OrderEndpoint {
    private final OrderService orderService;

    public OrderEndpoint(OrderService orderService) {
        this.orderService = orderService;
    }

    public void register(Javalin app) {
        app.post("/orders", ctx -> {
            try {
                CheckoutRequest request = ctx.bodyAsClass(CheckoutRequest.class);
                Order order = orderService.createOrder(request);
                ctx.status(201).json(order);
            } catch (CheckoutException e) {
                ctx.status(getHttpStatus(e.errorCode)).json(new ErrorResponse(e.errorCode));
            }
        });

        app.get("/orders/{id}", ctx -> {
            Order order = orderService.findOrder(ctx.pathParam("id"));
            if (order == null) {
                ctx.status(404).json(new ErrorResponse("ORDER_NOT_FOUND"));
                return;
            }
            ctx.status(200).json(order);
        });

        app.post("/orders/{id}/payment", ctx -> {
            try {
                PaymentRequest payment = ctx.bodyAsClass(PaymentRequest.class);
                Order order = orderService.processPayment(
                    ctx.pathParam("id"),
                    payment.transactionId,
                    payment.paidAt
                );
                ctx.status(200).json(order);
            } catch (OrderException e) {
                ctx.status(e.httpStatus).json(new ErrorResponse(e.errorCode));
            }
        });

        app.post("/orders/{id}/shipment", ctx -> {
            try {
                ShipmentRequest shipment = ctx.bodyAsClass(ShipmentRequest.class);
                Order order = orderService.shipOrder(ctx.pathParam("id"), shipment.trackingCode);
                ctx.status(200).json(order);
            } catch (OrderException e) {
                ctx.status(e.httpStatus).json(new ErrorResponse(e.errorCode));
            }
        });

        app.post("/orders/{id}/delivery", ctx -> {
            try {
                Order order = orderService.deliverOrder(ctx.pathParam("id"));
                ctx.status(200).json(order);
            } catch (OrderException e) {
                ctx.status(e.httpStatus).json(new ErrorResponse(e.errorCode));
            }
        });

        app.post("/orders/{id}/cancellation", ctx -> {
            try {
                Order order = orderService.cancelOrder(ctx.pathParam("id"));
                ctx.status(200).json(order);
            } catch (OrderException e) {
                ctx.status(e.httpStatus).json(new ErrorResponse(e.errorCode));
            }
        });
    }

    private int getHttpStatus(String errorCode) {
        return switch (errorCode) {
            case "CARRIER_UNAVAILABLE" -> 503;
            default -> 422;
        };
    }
}
