package com.loja.checkout.controller;

import com.loja.checkout.dto.RequisicaoPedido;
import com.loja.checkout.service.ServicoResumoPedido;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {
    private final ServicoResumoPedido servico;

    public CheckoutController(ServicoResumoPedido servico) {
        this.servico = servico;
    }

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody RequisicaoPedido req) {
        Object resultado = servico.calcularResumo(req);
        return ResponseEntity.ok(resultado);
    }
}
