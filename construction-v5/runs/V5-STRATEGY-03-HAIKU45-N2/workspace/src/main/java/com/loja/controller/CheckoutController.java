package com.loja.controller;

import com.loja.model.RequisicaoResumo;
import com.loja.service.ResumoCheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private ResumoCheckoutService resumoCheckoutService;

    @PostMapping("/resumo")
    public ResponseEntity<Object> calcularResumo(@RequestBody RequisicaoResumo requisicao) {
        Object resultado = resumoCheckoutService.calcularResumo(requisicao);
        return ResponseEntity.ok(resultado);
    }
}
