package com.loja.checkout.api;

import com.loja.checkout.api.dto.ResumoRequest;
import com.loja.checkout.api.dto.ResumoResponse;
import com.loja.checkout.servico.CalculoResumoService;
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
