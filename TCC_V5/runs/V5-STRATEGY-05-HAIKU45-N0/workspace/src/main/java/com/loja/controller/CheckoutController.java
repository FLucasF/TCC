package com.loja.controller;

import com.loja.dto.ErrorResponse;
import com.loja.dto.ResumoRequest;
import com.loja.dto.ResumoResponse;
import com.loja.service.ResumoCalculadorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    @Autowired
    private ResumoCalculadorService calculadorService;

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> resumo(@RequestBody ResumoRequest request) {
        try {
            ResumoResponse response = calculadorService.calcular(request);
            return ResponseEntity.ok(response);
        } catch (ResumoCalculadorService.ValidationException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getCode()));
        }
    }
}
