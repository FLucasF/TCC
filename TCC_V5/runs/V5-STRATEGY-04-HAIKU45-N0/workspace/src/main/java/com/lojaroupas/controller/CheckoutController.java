package com.lojaroupas.controller;

import com.lojaroupas.dto.CheckoutRequest;
import com.lojaroupas.dto.CheckoutResponse;
import com.lojaroupas.dto.ErrorResponse;
import com.lojaroupas.service.CheckoutException;
import com.lojaroupas.service.CheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    @PostMapping("/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody CheckoutRequest request) {
        try {
            CheckoutResponse response = checkoutService.calcularCheckout(request);
            return ResponseEntity.ok(response);
        } catch (CheckoutException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getCodigo()));
        }
    }

}
