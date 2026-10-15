package com.loja.checkout;

import com.loja.checkout.model.PedidoRequest;
import com.loja.checkout.model.PedidoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class CheckoutController {

    private final CheckoutService service;

    public CheckoutController(CheckoutService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> calcular(@RequestBody PedidoRequest req) {
        PedidoResponse resp = service.calcular(req);
        return ResponseEntity.ok(resp);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<Map<String, String>> handleCheckoutException(CheckoutException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(Map.of("erro", ex.getCodigo()));
    }
}
