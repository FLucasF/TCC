package com.loja.controller;

import com.loja.domain.OrderSummary;
import com.loja.dto.OrderRequest;
import com.loja.dto.OrderSummaryResponse;
import com.loja.service.CheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/summary")
    public ResponseEntity<OrderSummaryResponse> calculateSummary(@RequestBody OrderRequest request) {
        OrderSummary summary = checkoutService.calculateSummary(request);
        return ResponseEntity.ok(OrderSummaryResponse.from(summary));
    }
}
