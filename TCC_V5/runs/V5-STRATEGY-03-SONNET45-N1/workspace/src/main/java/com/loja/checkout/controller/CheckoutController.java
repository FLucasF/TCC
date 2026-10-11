package com.loja.checkout.controller;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ErroResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.CheckoutService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            ErroResponse erro = new ErroResponse(e.getCodigo().name());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
        }
    }
}
