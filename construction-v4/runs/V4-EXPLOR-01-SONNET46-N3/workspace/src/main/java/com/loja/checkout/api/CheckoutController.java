package com.loja.checkout.api;

import com.loja.checkout.dominio.checkout.ServicoCheckout;
import com.loja.checkout.infra.CheckoutException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final ServicoCheckout servico;

    public CheckoutController(ServicoCheckout servico) {
        this.servico = servico;
    }

    @PostMapping("/resumo")
    public ResumoResponse resumo(@RequestBody PedidoRequest pedido) {
        return servico.calcular(pedido);
    }

    @ExceptionHandler(CheckoutException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErroResponse handleCheckoutException(CheckoutException ex) {
        return new ErroResponse(ex.getCodigo());
    }
}
