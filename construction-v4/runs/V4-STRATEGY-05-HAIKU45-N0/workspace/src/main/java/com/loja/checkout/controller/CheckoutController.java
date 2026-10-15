package com.loja.checkout.controller;

import com.loja.checkout.domain.ResumoRequest;
import com.loja.checkout.domain.ResumoResponse;
import com.loja.checkout.service.ResumoCheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {
    @Autowired
    private ResumoCheckoutService resumoService;

    @PostMapping("/resumo")
    public ResumoResponse calcularResumo(@RequestBody ResumoRequest request) {
        return resumoService.calcularResumo(request);
    }
}
