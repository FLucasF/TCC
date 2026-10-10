package com.loja.checkout.controller;

import com.loja.checkout.dto.RequisicaoResumo;
import com.loja.checkout.service.CheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private CheckoutService checkoutService;

    @PostMapping(value = "/resumo", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> calcularResumo(@RequestBody RequisicaoResumo requisicao) {
        Object resultado = checkoutService.calcularResumo(requisicao);
        return ResponseEntity.ok(resultado);
    }
}
