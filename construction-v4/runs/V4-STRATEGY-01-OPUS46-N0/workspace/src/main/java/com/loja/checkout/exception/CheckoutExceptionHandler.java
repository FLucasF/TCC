package com.loja.checkout.exception;

import com.loja.checkout.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CheckoutExceptionHandler {

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErrorResponse> handleCheckout(CheckoutException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(new ErrorResponse(ex.getCodigo()));
    }
}
