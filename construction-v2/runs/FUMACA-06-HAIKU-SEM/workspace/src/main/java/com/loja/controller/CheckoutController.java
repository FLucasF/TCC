package com.loja.controller;

import com.loja.dto.ErroResponse;
import com.loja.dto.ResumoCheckoutRequest;
import com.loja.dto.ResumoCheckoutResponse;
import com.loja.service.CheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<?> resumo(@RequestBody ResumoCheckoutRequest request) {
        try {
            ResumoCheckoutResponse response = checkoutService.calcularResumo(request);
            return ResponseEntity.ok(response);
        } catch (CheckoutService.CheckoutException e) {
            return ResponseEntity.badRequest().body(new ErroResponse(e.getCodigo()));
        }
    }
}
