package com.loja.checkout.web;

import com.loja.checkout.servico.ResumoCompraService;
import com.loja.checkout.web.dto.ResumoRequestDTO;
import com.loja.checkout.web.dto.ResumoResponseDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final ResumoCompraService resumoCompraService;

    public CheckoutController(ResumoCompraService resumoCompraService) {
        this.resumoCompraService = resumoCompraService;
    }

    @PostMapping("/resumo")
    public ResumoResponseDTO resumo(@RequestBody ResumoRequestDTO request) {
        return resumoCompraService.calcular(request);
    }
}
