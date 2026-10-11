package com.loja.resumocompra.web;

import com.loja.resumocompra.PedidoRequest;
import com.loja.resumocompra.ResumoCompraService;
import com.loja.resumocompra.ResumoResponse;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoCompraController {

    private final ResumoCompraService service;

    public ResumoCompraController(ResumoCompraService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody PedidoRequest pedido) {
        return service.calcular(pedido);
    }
}
