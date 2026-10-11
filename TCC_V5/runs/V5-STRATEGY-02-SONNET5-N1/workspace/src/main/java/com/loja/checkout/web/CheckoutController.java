package com.loja.checkout.web;

import com.loja.checkout.dominio.CalculoResumoService;
import com.loja.checkout.web.dto.ResumoRequest;
import com.loja.checkout.web.dto.ResumoResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CalculoResumoService calculoResumoService;

    public CheckoutController(CalculoResumoService calculoResumoService) {
        this.calculoResumoService = calculoResumoService;
    }

    @PostMapping("/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest request) {
        return calculoResumoService.calcular(request);
    }
}
