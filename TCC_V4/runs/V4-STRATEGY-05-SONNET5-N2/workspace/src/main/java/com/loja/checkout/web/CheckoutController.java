package com.loja.checkout.web;

import com.loja.checkout.ResumoCompraService;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final ResumoCompraService resumoCompraService;

    public CheckoutController(ResumoCompraService resumoCompraService) {
        this.resumoCompraService = resumoCompraService;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest request) {
        return resumoCompraService.calcular(request);
    }
}
