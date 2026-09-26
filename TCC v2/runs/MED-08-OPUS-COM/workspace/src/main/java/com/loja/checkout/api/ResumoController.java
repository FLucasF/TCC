package com.loja.checkout.api;

import com.loja.checkout.ResumoDaCompraService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoController {

    private final ResumoDaCompraService servico;

    public ResumoController(ResumoDaCompraService servico) {
        this.servico = servico;
    }

    @PostMapping("/checkout/resumo")
    public ResponseEntity<ResumoResponse> resumo(@RequestBody(required = false) ResumoRequest pedido) {
        return ResponseEntity.ok(servico.calcular(pedido == null ? vazio() : pedido));
    }

    private ResumoRequest vazio() {
        return new ResumoRequest(null, null, null, null, null);
    }
}
