package com.loja.resumo.api;

import com.loja.resumo.calculo.Pedido;
import com.loja.resumo.calculo.ResumoCompra;
import com.loja.resumo.calculo.ResumoCompraService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class ResumoCompraController {

    private final ResumoCompraService service;

    public ResumoCompraController(ResumoCompraService service) {
        this.service = service;
    }

    @PostMapping("/resumo")
    public ResumoCompra resumo(@RequestBody Pedido pedido) {
        return service.calcular(pedido);
    }
}
