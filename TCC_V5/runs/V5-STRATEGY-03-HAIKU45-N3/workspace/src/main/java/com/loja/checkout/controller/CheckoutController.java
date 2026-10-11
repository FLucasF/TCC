package com.loja.checkout.controller;

import com.loja.checkout.model.*;
import com.loja.checkout.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final ResumoCalculadora resumoCalculadora;

    public CheckoutController() {
        this.resumoCalculadora = new ResumoCalculadora();
    }

    @PostMapping("/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody RequisicaoResumo requisicao) {
        try {
            RespostaResumo resposta = resumoCalculadora.calcular(requisicao);
            return ResponseEntity.ok(resposta);
        } catch (ValidacaoException e) {
            RespostaErro erro = new RespostaErro(e.getCodigoErro());
            return ResponseEntity.badRequest().body(erro);
        }
    }
}
