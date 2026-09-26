package com.loja.checkout.web;

import com.loja.checkout.ResumoCompraService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final ResumoCompraService servico;

    public CheckoutController(ResumoCompraService servico) {
        this.servico = servico;
    }

    @PostMapping("/resumo")
    public ResponseEntity<ResumoResponse> resumo(@RequestBody ResumoRequest requisicao) {
        return ResponseEntity.ok(servico.calcular(requisicao));
    }
}
