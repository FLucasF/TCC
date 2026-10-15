package com.loja.checkout.controller;

import com.loja.checkout.dto.RequisicaoResumo;
import com.loja.checkout.dto.RespostaErro;
import com.loja.checkout.dto.RespostaResumo;
import com.loja.checkout.servico.ResumoCheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {
    @Autowired
    private ResumoCheckoutService service;

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> resumo(@RequestBody RequisicaoResumo requisicao) {
        try {
            RespostaResumo resposta = service.calcular(requisicao);
            return ResponseEntity.ok(resposta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new RespostaErro(e.getMessage()));
        }
    }
}
