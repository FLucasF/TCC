package com.loja.checkout.api;

import com.loja.checkout.calculo.ResumoCompraService;
import org.springframework.http.MediaType;
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

    @PostMapping(path = "/resumo", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResumoCompraResponse resumo(@RequestBody ResumoCompraRequest requisicao) {
        return resumoCompraService.calcular(requisicao);
    }
}
