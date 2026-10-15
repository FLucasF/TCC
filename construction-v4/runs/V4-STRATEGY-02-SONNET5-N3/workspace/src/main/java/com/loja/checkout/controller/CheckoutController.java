package com.loja.checkout.controller;

import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.service.CalculadoraResumoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CalculadoraResumoService calculadoraResumoService;

    public CheckoutController(CalculadoraResumoService calculadoraResumoService) {
        this.calculadoraResumoService = calculadoraResumoService;
    }

    @PostMapping("/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest request) {
        return calculadoraResumoService.calcular(request);
    }
}
