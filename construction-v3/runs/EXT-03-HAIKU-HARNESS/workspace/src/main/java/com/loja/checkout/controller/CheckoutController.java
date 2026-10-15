package com.loja.checkout.controller;

import com.loja.checkout.dto.ErroResponse;
import com.loja.checkout.dto.RequisicaoCheckout;
import com.loja.checkout.dto.RespostaCheckout;
import com.loja.checkout.exception.ErroCheckout;
import com.loja.checkout.servico.CalculadoraCheckout;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CalculadoraCheckout calculadora;

    public CheckoutController(CalculadoraCheckout calculadora) {
        this.calculadora = calculadora;
    }

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> resumo(@RequestBody RequisicaoCheckout requisicao) {
        try {
            RespostaCheckout resposta = calculadora.calcular(requisicao);
            return ResponseEntity.ok(resposta);
        } catch (ErroCheckout e) {
            return ResponseEntity.badRequest().body(new ErroResponse(e.getCodigo()));
        }
    }
}
