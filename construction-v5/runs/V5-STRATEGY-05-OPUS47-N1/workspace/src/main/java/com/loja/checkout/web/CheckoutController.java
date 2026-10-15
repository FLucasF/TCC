package com.loja.checkout.web;

import com.loja.checkout.dominio.ErroPedido;
import com.loja.checkout.servico.ServicoResumo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CheckoutController {
    private final ServicoResumo servico;

    public CheckoutController(ServicoResumo servico) {
        this.servico = servico;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest req) {
        return servico.resumir(req);
    }

    @ExceptionHandler(ErroPedido.class)
    public ResponseEntity<Map<String, String>> tratarErro(ErroPedido erro) {
        return ResponseEntity.badRequest().body(Map.of("erro", erro.codigo()));
    }
}
