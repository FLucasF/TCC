package com.loja.checkout;

import com.loja.checkout.model.PedidoRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CheckoutController {

    private final CheckoutService service;

    public CheckoutController(CheckoutService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> resumo(@RequestBody PedidoRequest req) {
        return ResponseEntity.ok(service.calcular(req));
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<Map<String, String>> onCheckout(CheckoutException e) {
        return ResponseEntity.badRequest().body(Map.of("erro", e.codigo));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> onParse(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("erro", "PEDIDO_INVALIDO"));
    }
}
