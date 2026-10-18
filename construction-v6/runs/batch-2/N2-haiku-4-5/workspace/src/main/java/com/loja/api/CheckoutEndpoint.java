package com.loja.api;

import io.javalin.Javalin;
import com.loja.model.*;
import com.loja.service.CheckoutService;
import com.loja.service.CheckoutException;

public class CheckoutEndpoint {
    private final CheckoutService checkoutService;

    public CheckoutEndpoint(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    public void register(Javalin app) {
        app.post("/checkout/summary", ctx -> {
            try {
                CheckoutRequest request = ctx.bodyAsClass(CheckoutRequest.class);
                OrderSummary summary = checkoutService.calculateSummary(request);
                ctx.status(200).json(summary);
            } catch (CheckoutException e) {
                ctx.status(getHttpStatus(e.errorCode)).json(new ErrorResponse(e.errorCode));
            }
        });
    }

    private int getHttpStatus(String errorCode) {
        return 422;
    }
}
