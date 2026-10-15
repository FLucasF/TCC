package com.loja.checkout.api;

import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.servico.CheckoutServico;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutServico servico;

    public CheckoutController(CheckoutServico servico) {
        this.servico = servico;
    }

    @PostMapping("/resumo")
    public ResponseEntity<?> resumo(@RequestBody CheckoutRequest req) {
        CheckoutResponse response = servico.calcular(req);
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<Map<String, String>> handleCheckoutException(CheckoutException ex) {
        return ResponseEntity.unprocessableEntity()
                .body(Map.of("erro", ex.codigo.name()));
    }
}
