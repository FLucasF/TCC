package com.loja.checkout.controller;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ErroResponse;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.CheckoutService;
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
    public CheckoutResponse resumo(@RequestBody CheckoutRequest request) {
        return checkoutService.calcular(request);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> handleCheckoutException(CheckoutException ex) {
        return ResponseEntity.badRequest().body(new ErroResponse(ex.getErro()));
    }
}
