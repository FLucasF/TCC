package com.loja.checkout.web;

import com.loja.checkout.service.ResumoService;
import com.loja.checkout.web.dto.ResumoRequest;
import com.loja.checkout.web.dto.ResumoResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final ResumoService resumoService;

    public CheckoutController(ResumoService resumoService) {
        this.resumoService = resumoService;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest request) {
        return resumoService.calcular(request);
    }
}
