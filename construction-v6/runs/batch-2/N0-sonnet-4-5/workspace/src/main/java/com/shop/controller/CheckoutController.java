package com.shop.controller;

import com.shop.dto.CheckoutRequest;
import com.shop.dto.CheckoutSummary;
import com.shop.dto.ErrorResponse;
import com.shop.exception.BusinessException;
import com.shop.service.CheckoutService;
import io.javalin.http.Context;

public class CheckoutController {
    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    public void calculateSummary(Context ctx) {
        try {
            CheckoutRequest request = ctx.bodyAsClass(CheckoutRequest.class);
            CheckoutSummary summary = checkoutService.calculateSummary(request, null);
            ctx.status(200).json(summary);
        } catch (BusinessException e) {
            ctx.status(e.getHttpStatus()).json(new ErrorResponse(e.getErrorCode()));
        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR"));
        }
    }
}
