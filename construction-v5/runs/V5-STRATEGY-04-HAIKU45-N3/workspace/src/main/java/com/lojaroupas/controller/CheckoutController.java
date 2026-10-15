package com.lojaroupas.controller;

import com.lojaroupas.dto.RequisicaoCheckout;
import com.lojaroupas.dto.RespostaCheckout;
import com.lojaroupas.dto.RespostaErro;
import com.lojaroupas.servico.ResumoCheckoutService;
import com.lojaroupas.validador.ValidadorRequisicao;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final ResumoCheckoutService resumoService;
    private final ValidadorRequisicao validador;

    public CheckoutController(ResumoCheckoutService resumoService, ValidadorRequisicao validador) {
        this.resumoService = resumoService;
        this.validador = validador;
    }

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody RequisicaoCheckout requisicao) {
        String erro = validador.validar(requisicao);
        if (erro != null) {
            return ResponseEntity.ok(new RespostaErro(erro));
        }

        RespostaCheckout resposta = resumoService.calcular(requisicao);
        return ResponseEntity.ok(resposta);
    }
}
