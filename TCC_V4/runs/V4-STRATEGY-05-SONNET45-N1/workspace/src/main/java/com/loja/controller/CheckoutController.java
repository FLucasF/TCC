package com.loja.controller;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ErrorResponse;
import com.loja.exception.CheckoutException;
import com.loja.service.CheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody CheckoutRequest request) {
        try {
            CheckoutResponse response = checkoutService.calcularResumo(request);
            return ResponseEntity.ok(response);
        } catch (CheckoutException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getCodigo()));
        }
    }
}
