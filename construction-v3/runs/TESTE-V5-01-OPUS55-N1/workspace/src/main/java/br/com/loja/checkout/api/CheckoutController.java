package br.com.loja.checkout.api;

import br.com.loja.checkout.CheckoutService;
import br.com.loja.checkout.PedidoCheckout;
import br.com.loja.checkout.ResumoCompra;
import br.com.loja.checkout.pedido.CheckoutRecusadoException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/resumo")
    public ResumoCompra resumo(@RequestBody PedidoCheckout pedido) {
        return checkoutService.resumir(pedido);
    }

    @ExceptionHandler(CheckoutRecusadoException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ErroResposta recusado(CheckoutRecusadoException e) {
        return new ErroResposta(e.erro().name());
    }

    public record ErroResposta(String erro) {
    }
}
