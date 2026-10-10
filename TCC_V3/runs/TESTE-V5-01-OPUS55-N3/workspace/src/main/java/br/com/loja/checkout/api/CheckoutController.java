package br.com.loja.checkout.api;

import br.com.loja.checkout.CheckoutService;
import br.com.loja.checkout.PedidoRecusadoException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest pedido) {
        return checkoutService.resumir(pedido);
    }

    @ExceptionHandler(PedidoRecusadoException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ErroResponse recusado(PedidoRecusadoException e) {
        return new ErroResponse(e.codigo());
    }
}
