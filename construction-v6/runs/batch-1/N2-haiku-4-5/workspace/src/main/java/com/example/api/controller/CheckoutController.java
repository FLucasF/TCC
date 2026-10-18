package com.example.api.controller;

import com.example.api.dto.*;
import com.example.application.service.OrderService;
import com.example.domain.entity.OrderSummary;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<SummaryDto> summary(@RequestBody CheckoutRequestDto request) {
        OrderSummary summary = orderService.calculateSummary(request);
        SummaryDto dto = new SummaryDto(
                summary.getProductsSubtotal(),
                summary.getCouponDiscount(),
                summary.getShippingCost(),
                summary.getDeliveryDays(),
                summary.getInsurance(),
                summary.getPaymentAdjustment(),
                summary.getFinalTotal(),
                summary.getInstallments(),
                summary.getInstallmentAmount(),
                summary.getNextPurchaseCredit(),
                summary.getGift()
        );
        return ResponseEntity.ok(dto);
    }
}
