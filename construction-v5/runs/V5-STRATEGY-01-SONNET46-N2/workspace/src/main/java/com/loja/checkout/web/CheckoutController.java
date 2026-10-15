package com.loja.checkout.web;

import com.loja.checkout.service.CheckoutException;
import com.loja.checkout.service.CheckoutService;
import com.loja.checkout.web.dto.CheckoutRequest;
import com.loja.checkout.web.dto.CheckoutResponse;
import com.loja.checkout.web.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
    public CheckoutResponse resumo(@RequestBody CheckoutRequest request) {
        return checkoutService.processar(request);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErrorResponse> handleCheckoutException(CheckoutException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(ex.getCodigo().name()));
    }
}
