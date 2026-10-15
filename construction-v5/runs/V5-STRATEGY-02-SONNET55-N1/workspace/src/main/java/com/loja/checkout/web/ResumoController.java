package com.loja.checkout.web;

import com.loja.checkout.PedidoSolicitado;
import com.loja.checkout.ResumoPedido;
import com.loja.checkout.ResumoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoController {

    private final ResumoService service;

    public ResumoController(ResumoService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public ResumoPedido resumo(@RequestBody PedidoSolicitado pedido) {
        return service.calcular(pedido);
    }
}
