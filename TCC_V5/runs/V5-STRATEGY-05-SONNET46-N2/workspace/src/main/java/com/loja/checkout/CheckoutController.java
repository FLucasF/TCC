package com.loja.checkout;

import com.loja.checkout.model.CheckoutRequest;
import com.loja.checkout.model.CheckoutResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService service;

    public CheckoutController(CheckoutService service) {
        this.service = service;
    }

    @PostMapping("/resumo")
    public CheckoutResponse resumo(@RequestBody CheckoutRequest request) {
        return service.calcular(request);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<Map<String, String>> handleCheckoutException(CheckoutException ex) {
        return ResponseEntity.unprocessableEntity().body(Map.of("erro", ex.getCodigo()));
    }
}
