package com.loja.orderservice.controller;

import com.loja.orderservice.dto.CheckoutRequest;
import com.loja.orderservice.dto.SummaryResponse;
import com.loja.orderservice.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final OrderService orderService;

    public CheckoutController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/summary")
    public ResponseEntity<SummaryResponse> calculateSummary(@RequestBody CheckoutRequest request) {
        SummaryResponse summary = orderService.calculateSummary(request);
        return ResponseEntity.ok(summary);
    }
}
