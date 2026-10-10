package com.loja.checkout.controller;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.model.dto.CheckoutRequest;
import com.loja.checkout.model.dto.CheckoutResponse;
import com.loja.checkout.model.dto.ErrorResponse;
import com.loja.checkout.service.CheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    @PostMapping("/resumo")
    public ResponseEntity<?> resumo(@RequestBody CheckoutRequest request) {
        try {
            CheckoutResponse response = checkoutService.calcular(request);
            return ResponseEntity.ok(response);
        } catch (CheckoutException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getCodigo()));
        }
    }
}
