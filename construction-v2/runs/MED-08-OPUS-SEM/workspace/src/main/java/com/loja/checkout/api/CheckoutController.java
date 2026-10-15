package com.loja.checkout.api;

import com.loja.checkout.CheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CheckoutService servico;

    public CheckoutController(CheckoutService servico) {
        this.servico = servico;
    }

    @PostMapping("/checkout/resumo")
    public ResponseEntity<ResumoResponse> resumo(@RequestBody(required = false) ResumoRequest requisicao) {
        return ResponseEntity.ok(servico.calcular(requisicao));
    }
}
