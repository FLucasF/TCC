package com.loja.checkout.web;

import com.loja.checkout.calculo.ErroPedido;
import com.loja.checkout.calculo.ResumoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CheckoutController {

    private final ResumoService service;

    public CheckoutController(ResumoService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public CheckoutResponse resumo(@RequestBody CheckoutRequest req) {
        return service.calcular(req);
    }

    @ExceptionHandler(ErroPedido.class)
    public ResponseEntity<Map<String, String>> erro(ErroPedido e) {
        return ResponseEntity.badRequest().body(Map.of("erro", e.codigo()));
    }
}
