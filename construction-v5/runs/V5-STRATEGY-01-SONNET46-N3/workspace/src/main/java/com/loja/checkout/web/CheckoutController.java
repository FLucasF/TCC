package com.loja.checkout.web;

import com.loja.checkout.domain.CheckoutException;
import com.loja.checkout.domain.CheckoutService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService service;

    public CheckoutController(CheckoutService service) {
        this.service = service;
    }

    @PostMapping("/resumo")
    public ResumoResponse resumo(@RequestBody PedidoRequest pedido) {
        return service.calcular(pedido);
    }

    @ExceptionHandler(CheckoutException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErroResponse handleCheckoutException(CheckoutException ex) {
        return new ErroResponse(ex.getCodigo());
    }
}
