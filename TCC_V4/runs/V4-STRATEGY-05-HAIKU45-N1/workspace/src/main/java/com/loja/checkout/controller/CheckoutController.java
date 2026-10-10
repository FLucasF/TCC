package com.loja.checkout.controller;

import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.model.CheckoutRequest;
import com.loja.checkout.model.CheckoutResponse;
import com.loja.checkout.model.ErrorResponse;
import com.loja.checkout.service.CheckoutService;
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
    public ResponseEntity<?> calcularResumo(@RequestBody CheckoutRequest request) {
        try {
            CheckoutResponse response = checkoutService.calcularResumo(request);
            return ResponseEntity.ok(response);
        } catch (CheckoutException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getCodigoErro()));
        }
    }
}
