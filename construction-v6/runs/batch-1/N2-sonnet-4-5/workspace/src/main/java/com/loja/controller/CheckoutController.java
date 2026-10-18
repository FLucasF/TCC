package com.loja.controller;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.SummaryResponse;
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
    public ResponseEntity<SummaryResponse> calculateSummary(@RequestBody CheckoutRequest request) {
        SummaryResponse summary = checkoutService.calculateSummary(request);
        return ResponseEntity.ok(summary);
    }
}
