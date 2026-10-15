package com.loja.controller;

import com.loja.dto.ErroResposta;
import com.loja.dto.ResumoRequisicao;
import com.loja.dto.ResumoResposta;
import com.loja.service.ResumoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    @Autowired
    private ResumoService resumoService;

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody ResumoRequisicao requisicao) {
        try {
            ResumoResposta resposta = resumoService.calcularResumo(requisicao);
            return ResponseEntity.ok(resposta);
        } catch (ResumoService.ValidationException e) {
            return ResponseEntity.badRequest().body(new ErroResposta(e.getCodigo()));
        }
    }
}
