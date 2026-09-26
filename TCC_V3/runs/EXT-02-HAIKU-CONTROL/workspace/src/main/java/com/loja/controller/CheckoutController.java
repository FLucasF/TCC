package com.loja.controller;

import com.loja.dto.RequisicaoCheckout;
import com.loja.dto.RespostaErro;
import com.loja.service.CheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {
    @Autowired
    private CheckoutService checkoutService;

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> resumoCheckout(@RequestBody RequisicaoCheckout requisicao) {
        Object resultado = checkoutService.processarCheckout(requisicao);

        if (resultado instanceof RespostaErro) {
            return ResponseEntity.badRequest().body(resultado);
        }

        return ResponseEntity.ok(resultado);
    }
}
