package com.loja.checkout.controller;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ErrorResponse;
import com.loja.checkout.service.CheckoutService;
import com.loja.checkout.service.ValidacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private ValidacaoService validacaoService;

    @Autowired
    private CheckoutService checkoutService;

    @PostMapping("/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody CheckoutRequest request) {
        String erro = validacaoService.validar(request);
        if (erro != null) {
            return ResponseEntity.status(HttpStatus.OK).body(new ErrorResponse(erro));
        }

        CheckoutResponse response = checkoutService.calcular(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
