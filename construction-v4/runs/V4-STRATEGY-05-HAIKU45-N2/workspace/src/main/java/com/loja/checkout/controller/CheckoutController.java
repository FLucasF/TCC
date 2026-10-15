package com.loja.checkout.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.loja.checkout.dto.RequisicaoCheckout;
import com.loja.checkout.dto.RespostaCheckout;
import com.loja.checkout.dto.RespostaErro;
import com.loja.checkout.exception.ErroCheckout;
import com.loja.checkout.service.CheckoutService;

@RestController
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody RequisicaoCheckout requisicao) {
        try {
            RespostaCheckout resposta = checkoutService.calcularResumo(requisicao);
            return ResponseEntity.ok(resposta);
        } catch (ErroCheckout e) {
            return ResponseEntity.badRequest().body(new RespostaErro(e.getCodigo()));
        }
    }
}
