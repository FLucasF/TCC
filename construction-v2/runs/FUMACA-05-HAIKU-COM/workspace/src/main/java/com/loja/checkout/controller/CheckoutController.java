package com.loja.checkout.controller;

import com.loja.checkout.dto.ResumoCheckoutRequest;
import com.loja.checkout.dto.ResumoCheckoutResponse;
import com.loja.checkout.service.CalculadoraResumoService;
import com.loja.checkout.service.ValidacaoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    @Autowired
    private CalculadoraResumoService calculadoraResumoService;

    @PostMapping("/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody ResumoCheckoutRequest request) {
        try {
            ResumoCheckoutResponse response = calculadoraResumoService.calcular(request);
            return ResponseEntity.ok(response);
        } catch (ValidacaoException e) {
            return ResponseEntity.badRequest()
                .body(Collections.singletonMap("erro", e.getCodigoErro()));
        }
    }

}
