package com.loja.orderservice.controller;

import com.loja.orderservice.dto.CheckoutSummary;
import com.loja.orderservice.dto.CheckoutSummaryRequest;
import com.loja.orderservice.dto.ErrorResponse;
import com.loja.orderservice.exception.OrderServiceException;
import com.loja.orderservice.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {
    private final CheckoutService checkoutService;

    @PostMapping("/summary")
    public ResponseEntity<?> calculateSummary(@RequestBody CheckoutSummaryRequest request) {
        try {
            CheckoutSummary summary = checkoutService.calculateSummary(request);
            return ResponseEntity.ok(summary);
        } catch (OrderServiceException e) {
            return ResponseEntity.status(e.getHttpStatus())
                    .body(ErrorResponse.builder().error(e.getErrorCode()).build());
        }
    }
}
