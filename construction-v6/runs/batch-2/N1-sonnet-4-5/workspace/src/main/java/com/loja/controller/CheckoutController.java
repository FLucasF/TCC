package com.loja.controller;

import com.loja.domain.CheckoutRequest;
import com.loja.domain.CheckoutSummary;
import com.loja.service.CheckoutService;
import com.loja.util.ApiException;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.Map;

public class CheckoutController {
    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    public void calculateSummary(Context ctx) {
        try {
            CheckoutRequest request = ctx.bodyAsClass(CheckoutRequest.class);
            CheckoutSummary summary = checkoutService.calculateSummary(request);
            ctx.status(200).json(summary);
        } catch (ApiException e) {
            ctx.status(e.getHttpStatus()).json(Map.of("error", e.getErrorCode()));
        } catch (SQLException e) {
            ctx.status(500).json(Map.of("error", "INTERNAL_ERROR"));
        }
    }
}
