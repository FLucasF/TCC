package br.com.loja.checkout.api;

import br.com.loja.checkout.CheckoutException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final ResumoCompraService resumoCompra;

    public CheckoutController(ResumoCompraService resumoCompra) {
        this.resumoCompra = resumoCompra;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest pedido) {
        return resumoCompra.calcular(pedido);
    }

    @ExceptionHandler(CheckoutException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ErroResponse recusado(CheckoutException e) {
        return new ErroResponse(e.codigo());
    }
}
