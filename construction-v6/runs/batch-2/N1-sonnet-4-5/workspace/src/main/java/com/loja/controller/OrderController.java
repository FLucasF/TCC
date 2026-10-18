package com.loja.controller;

import com.loja.domain.CheckoutRequest;
import com.loja.domain.Order;
import com.loja.service.OrderService;
import com.loja.util.ApiException;
import io.javalin.http.Context;

import java.time.Instant;
import java.util.Map;

public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public void createOrder(Context ctx) {
        try {
            CheckoutRequest request = ctx.bodyAsClass(CheckoutRequest.class);
            Order order = orderService.createOrder(request);
            ctx.status(201).json(order);
        } catch (ApiException e) {
            ctx.status(e.getHttpStatus()).json(Map.of("error", e.getErrorCode()));
        }
    }

    public void getOrder(Context ctx) {
        try {
            String orderId = ctx.pathParam("id");
            Order order = orderService.getOrder(orderId);
            ctx.status(200).json(order);
        } catch (ApiException e) {
            ctx.status(e.getHttpStatus()).json(Map.of("error", e.getErrorCode()));
        }
    }

    public void processPayment(Context ctx) {
        try {
            String orderId = ctx.pathParam("id");
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String transactionId = (String) body.get("transactionId");
            String paidAtStr = (String) body.get("paidAt");
            Instant paidAt = paidAtStr != null ? Instant.parse(paidAtStr) : null;

            Order order = orderService.processPayment(orderId, transactionId, paidAt);
            ctx.status(200).json(order);
        } catch (ApiException e) {
            ctx.status(e.getHttpStatus()).json(Map.of("error", e.getErrorCode()));
        }
    }

    public void shipOrder(Context ctx) {
        try {
            String orderId = ctx.pathParam("id");
            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String trackingCode = body.get("trackingCode");

            Order order = orderService.shipOrder(orderId, trackingCode);
            ctx.status(200).json(order);
        } catch (ApiException e) {
            ctx.status(e.getHttpStatus()).json(Map.of("error", e.getErrorCode()));
        }
    }

    public void deliverOrder(Context ctx) {
        try {
            String orderId = ctx.pathParam("id");
            Order order = orderService.deliverOrder(orderId);
            ctx.status(200).json(order);
        } catch (ApiException e) {
            ctx.status(e.getHttpStatus()).json(Map.of("error", e.getErrorCode()));
        }
    }

    public void cancelOrder(Context ctx) {
        try {
            String orderId = ctx.pathParam("id");
            Order order = orderService.cancelOrder(orderId);
            ctx.status(200).json(order);
        } catch (ApiException e) {
            ctx.status(e.getHttpStatus()).json(Map.of("error", e.getErrorCode()));
        }
    }
}
