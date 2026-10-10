package com.loja.checkout.api;

import com.loja.checkout.CheckoutService;
import com.loja.checkout.PedidoCheckout;
import com.loja.checkout.dominio.ErroPedido;
import com.loja.checkout.dominio.PedidoRecusadoException;
import com.loja.checkout.dominio.ResumoCompra;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping(path = "/resumo", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResumoCompra resumo(@RequestBody(required = false) PedidoCheckout pedido) {
        if (pedido == null) {
            throw new PedidoRecusadoException(ErroPedido.PEDIDO_INVALIDO);
        }
        return checkoutService.calcular(pedido);
    }
}
