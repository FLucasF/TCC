package com.loja.controller;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ErrorResponse;
import com.loja.exception.CheckoutException;
import com.loja.service.CheckoutService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout/resumo")
    public ResponseEntity<CheckoutResponse> calcularResumo(@RequestBody CheckoutRequest request) {
        CheckoutResponse response = checkoutService.calcularResumo(request);
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErrorResponse> handleCheckoutException(CheckoutException e) {
        ErrorResponse error = new ErrorResponse(e.getCodigoErro());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
