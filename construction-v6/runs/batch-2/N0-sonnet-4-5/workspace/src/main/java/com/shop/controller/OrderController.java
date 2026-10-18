package com.shop.controller;

import com.shop.dto.*;
import com.shop.exception.BusinessException;
import com.shop.model.Order;
import com.shop.service.OrderService;
import io.javalin.http.Context;

public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public void createOrder(Context ctx) {
        try {
            CheckoutRequest request = ctx.bodyAsClass(CheckoutRequest.class);
            Order order = orderService.createOrder(request);
            ctx.status(201).json(toResponse(order));
        } catch (BusinessException e) {
            ctx.status(e.getHttpStatus()).json(new ErrorResponse(e.getErrorCode()));
        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR"));
        }
    }

    public void getOrder(Context ctx) {
        try {
            Long orderId = Long.parseLong(ctx.pathParam("id"));
            Order order = orderService.getOrder(orderId);
            ctx.status(200).json(toResponse(order));
        } catch (BusinessException e) {
            ctx.status(e.getHttpStatus()).json(new ErrorResponse(e.getErrorCode()));
        } catch (NumberFormatException e) {
            ctx.status(404).json(new ErrorResponse("ORDER_NOT_FOUND"));
        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR"));
        }
    }

    public void processPayment(Context ctx) {
        try {
            Long orderId = Long.parseLong(ctx.pathParam("id"));
            PaymentRequest request = ctx.bodyAsClass(PaymentRequest.class);

            if (request.getTransactionId() == null || request.getPaidAt() == null) {
                throw new BusinessException("INVALID_ORDER", 422);
            }

            Order order = orderService.processPayment(orderId, request.getTransactionId(), request.getPaidAt());
            ctx.status(200).json(toResponse(order));
        } catch (BusinessException e) {
            ctx.status(e.getHttpStatus()).json(new ErrorResponse(e.getErrorCode()));
        } catch (NumberFormatException e) {
            ctx.status(404).json(new ErrorResponse("ORDER_NOT_FOUND"));
        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR"));
        }
    }

    public void shipOrder(Context ctx) {
        try {
            Long orderId = Long.parseLong(ctx.pathParam("id"));
            ShipmentRequest request = ctx.bodyAsClass(ShipmentRequest.class);

            if (request.getTrackingCode() == null) {
                throw new BusinessException("INVALID_ORDER", 422);
            }

            Order order = orderService.shipOrder(orderId, request.getTrackingCode());
            ctx.status(200).json(toResponse(order));
        } catch (BusinessException e) {
            ctx.status(e.getHttpStatus()).json(new ErrorResponse(e.getErrorCode()));
        } catch (NumberFormatException e) {
            ctx.status(404).json(new ErrorResponse("ORDER_NOT_FOUND"));
        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR"));
        }
    }

    public void deliverOrder(Context ctx) {
        try {
            Long orderId = Long.parseLong(ctx.pathParam("id"));
            Order order = orderService.deliverOrder(orderId);
            ctx.status(200).json(toResponse(order));
        } catch (BusinessException e) {
            ctx.status(e.getHttpStatus()).json(new ErrorResponse(e.getErrorCode()));
        } catch (NumberFormatException e) {
            ctx.status(404).json(new ErrorResponse("ORDER_NOT_FOUND"));
        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR"));
        }
    }

    public void cancelOrder(Context ctx) {
        try {
            Long orderId = Long.parseLong(ctx.pathParam("id"));
            Order order = orderService.cancelOrder(orderId);
            ctx.status(200).json(toResponse(order));
        } catch (BusinessException e) {
            ctx.status(e.getHttpStatus()).json(new ErrorResponse(e.getErrorCode()));
        } catch (NumberFormatException e) {
            ctx.status(404).json(new ErrorResponse("ORDER_NOT_FOUND"));
        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR"));
        }
    }

    private OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setSummary(order.getSummary());
        response.setItems(order.getItems());
        response.setTrackingCode(order.getTrackingCode());
        return response;
    }
}
