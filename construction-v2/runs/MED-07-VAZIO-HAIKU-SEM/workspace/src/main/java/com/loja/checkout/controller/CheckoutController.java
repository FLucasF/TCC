package com.loja.checkout.controller;

import com.loja.checkout.error.CheckoutException;
import com.loja.checkout.error.ErrorResponse;
import com.loja.checkout.model.CheckoutRequest;
import com.loja.checkout.model.CheckoutResponse;
import com.loja.checkout.service.CheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    @PostMapping("/checkout/resumo")
    public ResponseEntity<CheckoutResponse> calcularResumo(@RequestBody CheckoutRequest request) {
        CheckoutResponse response = checkoutService.calcularResumo(request);
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErrorResponse> handleCheckoutException(CheckoutException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getCodigo());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
