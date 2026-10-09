package com.loja.checkout.api;

import com.loja.checkout.CalculoResumoService;
import com.loja.checkout.api.dto.ResumoRequest;
import com.loja.checkout.api.dto.ResumoResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoController {

    private final CalculoResumoService calculoResumoService;

    public ResumoController(CalculoResumoService calculoResumoService) {
        this.calculoResumoService = calculoResumoService;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest request) {
        return calculoResumoService.calcular(request);
    }
}
