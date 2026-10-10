package com.loja.checkout;

import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.service.ResumoCompraService;
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
    public ResumoResponse resumo(@RequestBody PedidoRequest pedido) {
        return resumoCompraService.calcular(pedido);
    }
}
