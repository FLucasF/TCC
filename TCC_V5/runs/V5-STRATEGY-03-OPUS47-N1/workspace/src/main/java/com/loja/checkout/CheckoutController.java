package com.loja.checkout;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final ResumoService service;

    public CheckoutController(ResumoService service) { this.service = service; }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest req) {
        return service.calcular(req);
    }

    @ExceptionHandler(ErroPedido.class)
    public ResponseEntity<Map<String, String>> onErro(ErroPedido e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", e.codigo()));
    }
}
