package com.loja.checkout.controller;

import com.loja.checkout.domain.RequisicaoResumo;
import com.loja.checkout.domain.RespostaResumo;
import com.loja.checkout.exception.ErroCheckout;
import com.loja.checkout.service.ServicoResumoCheckout;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/checkout")
public class ControllerCheckout {
    private final ServicoResumoCheckout servico = new ServicoResumoCheckout();

    @PostMapping("/resumo")
    public ResponseEntity<?> obterResumo(@RequestBody RequisicaoResumo requisicao) {
        try {
            RespostaResumo resposta = servico.calcularResumo(requisicao);
            return ResponseEntity.ok(resposta);
        } catch (ErroCheckout e) {
            Map<String, String> erro = new HashMap<>();
            erro.put("erro", e.getCodigo());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
        }
    }
}
