package com.loja.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.loja.model.CheckoutRequest;
import com.loja.model.CheckoutResponse;
import com.loja.model.ErrorResponse;
import com.loja.service.CheckoutService;
import com.loja.service.ValidationException;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    @PostMapping("/resumo")
    public ResponseEntity<?> calculateCheckout(@RequestBody CheckoutRequest request) {
        try {
            CheckoutResponse response = checkoutService.calculateCheckout(request);
            return ResponseEntity.ok(response);
        } catch (ValidationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getErrorCode()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("ERRO_INTERNO"));
        }
    }
}
